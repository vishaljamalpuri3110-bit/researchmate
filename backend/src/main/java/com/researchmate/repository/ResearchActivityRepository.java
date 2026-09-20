package com.researchmate.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.researchmate.entity.ResearchActivity;

public interface ResearchActivityRepository extends JpaRepository<ResearchActivity, Long> {

    List<ResearchActivity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);
}
