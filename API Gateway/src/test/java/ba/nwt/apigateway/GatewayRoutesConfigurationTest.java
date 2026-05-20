package ba.nwt.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayRoutesConfigurationTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void resourceServiceRouteUsesLoadBalancedUri() {
        List<RouteDefinition> definitions = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(definitions).isNotNull();
        RouteDefinition resourceRoute = definitions.stream()
                .filter(definition -> "resource-service".equals(definition.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(resourceRoute.getUri().toString()).isEqualTo("lb://resource-service");
    }
}
