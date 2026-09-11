package com.integration.bridge.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GetAllCustomersRequest", namespace = "http://integration.com/systemb/customer")
@XmlType(namespace = "http://integration.com/systemb/customer")
public class GetAllCustomersRequest {

    @XmlElement
    private String lastModifiedSince;

    public String getLastModifiedSince() { return lastModifiedSince; }
    public void setLastModifiedSince(String lastModifiedSince) { this.lastModifiedSince = lastModifiedSince; }
}