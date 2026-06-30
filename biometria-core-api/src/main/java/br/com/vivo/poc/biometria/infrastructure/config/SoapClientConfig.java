package br.com.vivo.poc.biometria.infrastructure.config;

import jakarta.xml.soap.MessageFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.time.Duration;

@Configuration
public class SoapClientConfig {

    @Value("${soap.legacy.url}")
    private String soapUrl;

    @Value("${soap.legacy.timeout.connect-ms}")
    private int connectTimeout;

    @Value("${soap.legacy.timeout.read-ms}")
    private int readTimeout;

    @Bean
    public WebServiceTemplate webServiceTemplate() throws Exception {
        SaajSoapMessageFactory messageFactory = new SaajSoapMessageFactory(MessageFactory.newInstance());
        messageFactory.afterPropertiesSet();

        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(Duration.ofMillis(connectTimeout));
        sender.setReadTimeout(Duration.ofMillis(readTimeout));

        WebServiceTemplate template = new WebServiceTemplate(messageFactory);
        template.setMessageSender(sender);
        template.setDefaultUri(soapUrl);
        return template;
    }
}
