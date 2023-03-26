package com.se1605.service.impl;

import com.se1605.entity.FileEntity;
import com.se1605.entity.User;
import com.se1605.repository.FileRepository;
import com.se1605.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class FileServiceImpl implements FileService {
    @Autowired
    private FileRepository fileRepository;


    @Override
    public Optional<FileEntity> findById(long id) {
        return fileRepository.findById(id);
    }

    @Override
    public List<FileEntity> findByUser(String email) {
        return fileRepository.findAllByUserEmail(email);
    }

    @Override
    public void storeFile(MultipartFile file, User user) {
        String filename = file.getOriginalFilename();
        String path = "/path/to/files/" + filename;
        // save file metadata with user ID
        fileRepository.save(FileEntity.builder().user(user).filename(filename).path(path).build());
        // save file to disk
        try {
            file.transferTo(new java.io.File(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
