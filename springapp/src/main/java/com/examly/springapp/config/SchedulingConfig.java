package com.examly.springapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled jobs (e.g. removing unverified signups after 24 hours). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}