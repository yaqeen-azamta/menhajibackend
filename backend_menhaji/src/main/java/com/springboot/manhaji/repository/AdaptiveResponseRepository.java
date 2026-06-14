package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.AdaptiveResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdaptiveResponseRepository extends JpaRepository<AdaptiveResponse, Long> {

    List<AdaptiveResponse> findByAttemptId(Long attemptId);
}
