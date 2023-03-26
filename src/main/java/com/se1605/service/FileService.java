package com.se1605.service;

import com.se1605.entity.FileEntity;
import com.se1605.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface FileService {
    Optional<FileEntity> findById(long id);

    List<FileEntity> findByUser(String email);

    public void storeFile(MultipartFile file, User user);
}
