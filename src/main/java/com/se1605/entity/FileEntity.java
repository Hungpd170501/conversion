package com.se1605.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "files")
public class FileEntity {
    @Id
    private Long id;

    @ManyToOne
    private User user;

    private String filename;
    private String path;
}
