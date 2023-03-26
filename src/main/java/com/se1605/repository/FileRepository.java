package com.se1605.repository;

import com.se1605.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    @Query("select f from FileEntity f join User u where u.email = :email")
    List<FileEntity> findAllByUserEmail(@Param("email") String email);
}
