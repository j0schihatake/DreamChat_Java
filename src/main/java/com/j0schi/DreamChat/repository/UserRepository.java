package com.j0schi.DreamChat.repository;

import com.j0schi.DreamChat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByDeviceId(String deviceId);
    boolean existsByPhoneNumber(String phoneNumber);
}