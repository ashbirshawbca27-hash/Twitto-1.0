package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostDTO {
    private Long id;
    private Long userId;
    private String username;
    private String content;
    private long timestamp;
    private int heartCount;
    private int laughCount;
    private String role;
}
