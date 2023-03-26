package com.se1605.service;

import com.se1605.dto.UserDto;
import com.se1605.entity.User;

import java.util.List;

public interface UserService {
    void saveUser(UserDto userDto);

    User findByEmail(String email);

    List<UserDto> findAllUsers();
}
