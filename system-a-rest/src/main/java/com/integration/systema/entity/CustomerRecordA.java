package com.integration.systema.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer_record_a")
public class CustomerRecordA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String emailAddress;

    @Column(nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public CustomerRecordA() {}

    public CustomerRecordA(String fullName, String emailAddress, String phoneNumber) {
        this.fullName = fullName;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
