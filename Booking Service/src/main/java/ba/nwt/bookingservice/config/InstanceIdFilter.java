package ba.nwt.bookingservice.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class InstanceIdFilter implements Filter {

    @Value("${spring.application.name:unknown}")
    private String appName;

    @Value("${server.port:0}")
    private String port;

    private String hostname() {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (UnknownHostException e) { return "unknown-host"; }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (res instanceof HttpServletResponse http) {
            http.setHeader("X-Instance-Id", appName + "@" + hostname() + ":" + port);
        }
        chain.doFilter(req, res);
    }
}
