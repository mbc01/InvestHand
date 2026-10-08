package com.investment.Group.management.Repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.investment.Group.management.model.Contribution;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    // it is used to Find all contributions for a specific member
    List<Contribution> findByMemberId(Long memberId);

    // it is used to find contributions by month
    List<Contribution> findByMonth(String month);

    // Check if a member already paid for a given month
    boolean existsByMemberIdAndMonth(Long memberId, String month);
}

