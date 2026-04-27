package ba.nwt.bookingservice.config;

import ba.nwt.bookingservice.exception.DownstreamBadRequestException;
import ba.nwt.bookingservice.exception.DownstreamUnavailableException;
import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Per-Feign-client config: connect/read timeouts and an ErrorDecoder that maps
 * downstream HTTP errors into typed exceptions handled by GlobalExceptionHandler.
 *
 * NOTE: this class is intentionally NOT @Configuration — Spring Cloud OpenFeign
 * applies it per @FeignClient via the `configuration` attribute.
 */
public class FeignConfig {

    private static final Logger log = LoggerFactory.getLogger(FeignConfig.class);

    @Bean
    public Request.Options feignRequestOptions() {
        // connect 2s, read 5s — fast enough to fail circuit breaker quickly.
        return new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new TypedErrorDecoder();
    }

    static class TypedErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            String service = serviceFromMethodKey(methodKey);
            String body = readBody(response);
            int status = response.status();
            log.warn("Feign {} returned {} for {}: {}", service, status, methodKey, body);

            if (status >= 500 || status == 503 || status == 504) {
                return new DownstreamUnavailableException(service,
                        service + " returned " + status + ": " + body);
            }
            if (status >= 400 && status < 500) {
                return new DownstreamBadRequestException(service, status,
                        service + " rejected request (" + status + "): " + body);
            }
            return defaultDecoder.decode(methodKey, response);
        }

        private String serviceFromMethodKey(String methodKey) {
            if (methodKey == null) return "downstream";
            int hash = methodKey.indexOf('#');
            String simple = hash > 0 ? methodKey.substring(0, hash) : methodKey;
            int dot = simple.lastIndexOf('.');
            String cls = dot > 0 ? simple.substring(dot + 1) : simple;
            // ResourceServiceClient -> resource-service
            return cls.replace("ServiceClient", "")
                    .replaceAll("([a-z])([A-Z])", "$1-$2")
                    .toLowerCase() + "-service";
        }

        private String readBody(Response response) {
            try {
                if (response.body() == null) return "";
                byte[] bytes = response.body().asInputStream().readAllBytes();
                String s = new String(bytes, StandardCharsets.UTF_8);
                return s.length() > 500 ? s.substring(0, 500) + "..." : s;
            } catch (IOException e) {
                return "<unreadable body>";
            }
        }
    }
}
