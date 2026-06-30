package br.com.vivo.poc.legado.ws.config;

import br.com.vivo.poc.legado.ws.security.InternalSecretInterceptor;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.server.endpoint.SimpleSoapExceptionResolver;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import java.util.List;

@EnableWs
@Configuration
public class WsConfig extends WsConfigurerAdapter {

    public static final String NAMESPACE_URI = "http://vivo.com.br/poc/biometria";

    private final InternalSecretInterceptor internalSecretInterceptor;

    public WsConfig(InternalSecretInterceptor internalSecretInterceptor) {
        this.internalSecretInterceptor = internalSecretInterceptor;
    }

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "biometria")
    public DefaultWsdl11Definition biometriaWsdl11Definition(XsdSchema biometriaSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("BiometriaPort");
        definition.setLocationUri("/ws");
        definition.setTargetNamespace(NAMESPACE_URI);
        definition.setSchema(biometriaSchema);
        return definition;
    }

    @Bean
    public XsdSchema biometriaSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/biometria.xsd"));
    }

    @Bean
    public SimpleSoapExceptionResolver exceptionResolver() {
        return new SimpleSoapExceptionResolver();
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(internalSecretInterceptor);
    }
}
