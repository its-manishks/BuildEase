package com.examly.springapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.examly.springapp.model.ErrorLog;

@Repository
public interface ErrorLogRepo extends JpaRepository<ErrorLog, Long> {

    /** Most recent errors first - handy when debugging from the MySQL console or a future admin view. */
    List<ErrorLog> findAllByOrderByTimestampDesc();
}