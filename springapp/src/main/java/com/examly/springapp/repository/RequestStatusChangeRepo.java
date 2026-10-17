package com.examly.springapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.examly.springapp.model.RequestStatusChange;

@Repository
public interface RequestStatusChangeRepo extends JpaRepository<RequestStatusChange, Long> {

    /** A request's decisions in the order they happened (the timeline). */
    List<RequestStatusChange> findByMaterialRequestIdOrderByChangedAtAsc(Long materialRequestId);
}