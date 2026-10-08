package com.investment.Group.management.model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;


    @Entity
    @Table(name = "members")
    public class Member {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @OneToOne
        @JoinColumn(name = "user_id", nullable = false, unique = true)
        private User user;

        @Column(nullable = false)
        private String fullName;

        // Percentage share of the group investment. BigDecimal (not double)
        // so that shares can be validated to total exactly 100% without
        // floating point drift.
        @Column(nullable = false, precision = 5, scale = 2)
        private BigDecimal sharePercentage;

        private LocalDate joinDate;

        @Column(nullable = false)
        private String status; // ACTIVE, SUSPENDED

        // cascade = ALL means deleting a Member also deletes their
        // Contributions (orphanRemoval would do the same more precisely).
        @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Contribution> contributions = new ArrayList<>();

        // Constructors
        public Member() {}

        public Member(User user, String fullName, BigDecimal sharePercentage, String status) {
            this.user = user;
            this.fullName = fullName;
            this.sharePercentage = sharePercentage;
            this.status = status;
        }

        /**
         * JPA lifecycle hook. Preferring @PrePersist over a field initializer
         * means the date is assigned by Hibernate at INSERT time, so a detached
         * entity round-tripped through JSON cannot carry a bogus join date.
         */
        @PrePersist
        void onCreate() {
            if (this.joinDate == null) {
                this.joinDate = LocalDate.now();
            }
        }

        // Getters & Setters
        public Long getId() {
            return id;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public BigDecimal getSharePercentage() {
            return sharePercentage;
        }

        public void setSharePercentage(BigDecimal sharePercentage) {
            this.sharePercentage = sharePercentage;
        }

        public LocalDate getJoinDate() {
            return joinDate;
        }

        public void setJoinDate(LocalDate joinDate) {
            this.joinDate = joinDate;
        }

        /**
         * Defensive copy of the contributions list. Returning the live list
         * would let callers mutate the managed collection directly, bypassing
         * Hibernate's dirty checking.
         */
        public List<Contribution> getContributions() {
            return contributions == null ? List.of() : List.copyOf(contributions);
        }

        public void addContribution(Contribution contribution) {
            if (contributions == null) {
                contributions = new ArrayList<>();
            }
            contributions.add(contribution);
            contribution.setMember(this);
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }


