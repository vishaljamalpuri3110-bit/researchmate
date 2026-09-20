package com.researchmate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.researchmate.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);
}
