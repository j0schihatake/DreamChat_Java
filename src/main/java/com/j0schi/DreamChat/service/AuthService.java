package com.j0schi.DreamChat.service;

import com.j0schi.DreamChat.enums.UserStatus;
import com.j0schi.DreamChat.model.AuthRequest;
import com.j0schi.DreamChat.model.AuthResponse;
import com.j0schi.DreamChat.model.User;
import com.j0schi.DreamChat.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public AuthResponse handleAuthRequest(AuthRequest request) {
        String phoneNumber = normalizePhoneNumber(request.getPhoneNumber());

        Optional<User> existingUser = userRepository.findByDeviceId(request.getDeviceId());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setAuthorized(true);
            userRepository.save(user);
            return new AuthResponse(true, user.getId(), user.getUsername(), "Авторизация успешна");
        }

        Optional<User> userByPhone = userRepository.findByPhoneNumber(phoneNumber);
        if (userByPhone.isPresent()) {
            User user = userByPhone.get();
            user.setDeviceId(request.getDeviceId());
            user.setAuthorized(true);
            userRepository.save(user);
            return new AuthResponse(true, user.getId(), user.getUsername(), "Авторизация успешна");
        }

        User newUser = new User();
        newUser.setPhoneNumber(phoneNumber);
        newUser.setDeviceId(request.getDeviceId());
        newUser.setUsername("User_" + UUID.randomUUID().toString().substring(0, 8));
        newUser.setAuthorized(true);
        newUser.setStatus(com.j0schi.DreamChat.enums.UserStatus.ONLINE);

        userRepository.save(newUser);

        return new AuthResponse(true, newUser.getId(), newUser.getUsername(), "Пользователь создан и авторизован");
    }

    public AuthResponse checkAuthStatus(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);
        Optional<User> user = userRepository.findByPhoneNumber(normalizedPhone);

        if (user.isPresent() && user.get().isAuthorized()) {
            return new AuthResponse(true, user.get().getId(), user.get().getUsername(), "Авторизован");
        }else{
            // Только для тестов дальше надо добавить логику регистрации в телеграм.
            User newUser = new User();
            newUser.setStatus(UserStatus.AWAY);
            newUser.setPhoneNumber(phoneNumber);
            newUser.setDeviceId("phone" + phoneNumber);
            userRepository.save(newUser);
            return new AuthResponse(true, user.get().getId(), user.get().getUsername(), "Авторизован");
        }

        //return new AuthResponse(false, "Не авторизован");
    }

    private String normalizePhoneNumber(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}