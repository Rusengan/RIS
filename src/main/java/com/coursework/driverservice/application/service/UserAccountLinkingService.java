package com.coursework.driverservice.application.service;

import com.coursework.driverservice.infrastructure.external.googleauth.GoogleIdTokenPayload;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;

public interface UserAccountLinkingService {

    /**
     * Finds an existing user by Google subject or email, or creates a new one (default role DRIVER).
     */
    UserEntity findOrCreate(GoogleIdTokenPayload payload);
}
