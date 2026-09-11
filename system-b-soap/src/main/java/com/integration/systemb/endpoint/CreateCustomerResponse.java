package com.integration.systemb.endpoint;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CreateCustomerResponse", namespace = "http://integration.com/systemb/customer")
@XmlType(namespace = "http://integration.com/systemb/customer")
public class CreateCustomerResponse {

    @XmlElement
    private CustomerRecordBType customer;

    public CustomerRecordBType getCustomer() { return customer; }
    public void setCustomer(CustomerRecordBType customer) { this.customer = customer; }
}