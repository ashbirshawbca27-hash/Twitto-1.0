package com.example.demo.repository;

import com.example.demo.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // Get all comments for a specific post, ordered by creation date
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    
    // Get all comments for a specific post (descending order)
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);
}

