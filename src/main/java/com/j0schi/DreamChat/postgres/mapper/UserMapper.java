package com.j0schi.DreamChat.postgres.mapper;

import com.j0schi.DreamChat.model.User;
import com.j0schi.DreamChat.enums.UserStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Component
public class UserMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setStatus(UserStatus.valueOf(rs.getString("status")));
        user.setLastSeen(rs.getObject("last_seen", LocalDateTime.class));
        user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        user.setDeviceId(rs.getString("device_id"));
        user.setAuthorized(rs.getBoolean("is_authorized"));

        return user;
    }
}