package com.integration.systemb.endpoint;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GetCustomerByNumberRequest", namespace = "http://integration.com/systemb/customer")
@XmlType(namespace = "http://integration.com/systemb/customer")
public class GetCustomerByNumberRequest {

    @XmlElement
    private Long customerNumber;

    public Long getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(Long customerNumber) { this.customerNumber = customerNumber; }
}