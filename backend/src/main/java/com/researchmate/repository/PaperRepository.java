package com.researchmate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.researchmate.entity.Paper;

public interface PaperRepository extends JpaRepository<Paper,Long>{
    List<Paper> findByOwnerId(Long ownerId);

    Optional<Paper> findByIdAndOwnerId(Long paperId, Long ownerId);
}
