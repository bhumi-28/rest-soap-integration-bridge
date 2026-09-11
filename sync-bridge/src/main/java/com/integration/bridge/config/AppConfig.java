package com.integration.bridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SaajSoapMessageFactory soapMessageFactory() {
        return new SaajSoapMessageFactory();
    }

    @Bean
    public Jaxb2Marshaller soapMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("com.integration.bridge.soap");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller soapMarshaller,
                                                 SaajSoapMessageFactory soapMessageFactory,
                                                 @Value("${system-b.base-url}") String baseUrl) {
        WebServiceTemplate template = new WebServiceTemplate(soapMessageFactory);
        template.setMarshaller(soapMarshaller);
        template.setUnmarshaller(soapMarshaller);
        template.setDefaultUri(baseUrl + "/ws");
        template.setMessageSender(new HttpUrlConnectionMessageSender());
        return template;
    }
}