package com.integration.systemb.endpoint;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CreateCustomerRequest", namespace = "http://integration.com/systemb/customer")
@XmlType(namespace = "http://integration.com/systemb/customer")
public class CreateCustomerRequest {

    @XmlElement
    private String name;

    @XmlElement
    private String contactEmail;

    @XmlElement
    private String phone;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}