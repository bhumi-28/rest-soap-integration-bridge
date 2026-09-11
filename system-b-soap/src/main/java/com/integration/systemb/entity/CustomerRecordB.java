package com.integration.systemb.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer_record_b")
public class CustomerRecordB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerNumber;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String contactEmail;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false)
    private Instant lastModified;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastModified = Instant.now();
    }

    public CustomerRecordB() {}

    public CustomerRecordB(String name, String contactEmail, String phone) {
        this.name = name;
        this.contactEmail = contactEmail;
        this.phone = phone;
    }

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
