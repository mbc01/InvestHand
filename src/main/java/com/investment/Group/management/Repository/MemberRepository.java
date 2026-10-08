package com.investment.Group.management.Repository;



import com.investment.Group.management.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}  

