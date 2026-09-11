package com.integration.systemb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@Configuration
public class SoapConfig {

    @Bean
    public XsdSchema customerSchema() {
        return new SimpleXsdSchema(new ClassPathResource("customer.xsd"));
    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.integration.systemb.endpoint");
        return marshaller;
    }

    @Bean(name = "customer")
    public DefaultWsdl11Definition defaultWsdl11Definition() {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("CustomerPort");
        definition.setLocationUri("/ws");
        definition.setTargetNamespace("http://integration.com/systemb/customer");
        definition.setSchema(customerSchema());
        return definition;
    }
}