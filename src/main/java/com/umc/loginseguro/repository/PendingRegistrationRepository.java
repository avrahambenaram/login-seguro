package com.umc.loginseguro.repository;

import com.umc.loginseguro.entity.PendingRegistration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PendingRegistrationRepository extends MongoRepository<PendingRegistration, String> {
    boolean existsByUsernameIgnoreCase(String username);
}
