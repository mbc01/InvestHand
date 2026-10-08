package com.investment.Group.management.model;


import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 💰 Amount contributed.
    // BigDecimal, not double: binary floating point cannot represent values
    // like 0.10 exactly, so summing monetary amounts with double accumulates
    // rounding error. BigDecimal with a fixed scale is exact for money.
    // Declared as decimal(19,4) so fractional currency units are retained.
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // 📅 Date when payment was made
    private LocalDate paymentDate;

    // 📆 Month of contribution (e.g., "JANUARY")
    //
    // Column name left as the implicit "month" to match the existing
    // production schema, which is correct: MONTH is an unreserved keyword in
    // PostgreSQL (confirmed via pg_get_keywords), so "month varchar(255)" is
    // valid there.
    //
    // H2, the in-memory database the tests run against, does reserve MONTH and
    // rejects the column. That is handled in the test datasource
    // (NON_KEYWORDS=MONTH) rather than here, so production is not renamed to
    // satisfy a test-only database.
    private String month;

    // ⚠️ Status (PAID, LATE, MISSED)
    private String status;

    // 👤 Relationship: Many contributions belong to one member
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // ✅ Default constructor (REQUIRED by JPA)
    public Contribution() {}

    // Constructor
    public Contribution(BigDecimal amount, LocalDate paymentDate, String month, String status, Member member) {
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.month = month;
        this.status = status;
        this.member = member;
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }
}