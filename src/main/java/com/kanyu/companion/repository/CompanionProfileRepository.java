package com.kanyu.companion.repository;

import com.kanyu.companion.model.CompanionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanionProfileRepository extends JpaRepository<CompanionProfile, Long> {
    
    Optional<CompanionProfile> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
