package com.integration.bridge.dto;

import java.time.Instant;

public class CustomerRecordBDto {
    private Long customerNumber;
    private String name;
    private String contactEmail;
    private String phone;
    private Instant lastModified;

    public CustomerRecordBDto() {}

    public Long getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(Long customerNumber) { this.customerNumber = customerNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Instant getLastModified() { return lastModified; }
    public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
}
