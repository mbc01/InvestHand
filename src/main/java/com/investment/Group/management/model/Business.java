package com.investment.Group.management.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "businesses")
public class Business {

    //  Primary Key for the table
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // the name of the Business
    @Column(nullable = false)
    private String name;

    //  Type of business (e.g., Farming, Retail)
    private String type;

    // Capital invested. BigDecimal, not double, to keep monetary values exact.
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal capital;

    //  Start date
    private LocalDate startDate;

    //  Description of business
    private String description;

    //  Default constructor (REQUIRED by JPA)
    public Business() {}

    // Constructor
    public Business(String name, String type, BigDecimal capital, LocalDate startDate, String description) {
        this.name = name;
        this.type = type;
        this.capital = capital;
        this.startDate = startDate;
        this.description = description;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getCapital() {
        return capital;
    }

    public void setCapital(BigDecimal capital) {
        this.capital = capital;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
