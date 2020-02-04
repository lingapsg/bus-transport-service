package se.transport.bus.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import javax.annotation.PreDestroy;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class WireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static WireMockServer wireMockServer;

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        Properties props = new Properties();
        props.put("wiremock.port", configureWireMock());
        configurableApplicationContext.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource(this.getClass().getName(), props));
    }

    private static int configureWireMock() {
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(options().dynamicPort().enableBrowserProxying(true));
            wireMockServer.start();
            int port = wireMockServer.port();
            WireMock.configureFor("localhost", port);
        }
        return wireMockServer.port();
    }

    @PreDestroy
    public void resetWiremock() {
        wireMockServer.stop();
    }
}
