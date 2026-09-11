package com.integration.bridge.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "UpdateCustomerRequest", namespace = "http://integration.com/systemb/customer")
@XmlType(namespace = "http://integration.com/systemb/customer")
public class UpdateCustomerRequest {

    @XmlElement
    private Long customerNumber;

    @XmlElement
    private String name;

    @XmlElement
    private String contactEmail;

    @XmlElement
    private String phone;

    public Long getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(Long customerNumber) { this.customerNumber = customerNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}