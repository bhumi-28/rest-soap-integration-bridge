package com.integration.systemb.endpoint;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GetAllCustomersResponse", namespace = "http://integration.com/systemb/customer")
@XmlType(namespace = "http://integration.com/systemb/customer")
public class GetAllCustomersResponse {

    @XmlElementWrapper(name = "customers", namespace = "http://integration.com/systemb/customer")
    @XmlElement(name = "customer", namespace = "http://integration.com/systemb/customer")
    private List<CustomerRecordBType> customers;

    public List<CustomerRecordBType> getCustomers() { return customers; }
    public void setCustomers(List<CustomerRecordBType> customers) { this.customers = customers; }
}