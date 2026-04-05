package com.example.demo.mysocial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Comment;
import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentAPIController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * GET all comments for a specific post
     * GET /api/comments/post/{postId}
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Map<String, Object>>> getCommentsByPost(@PathVariable Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        
        List<Map<String, Object>> response = comments.stream().map(c -> {
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("id", c.getId());
            commentMap.put("postId", c.getPost().getId());
            commentMap.put("userId", c.getUser().getId());
            commentMap.put("author", c.getUser().getUsername());
            commentMap.put("avatar", c.getUser().getUsername().charAt(0));
            commentMap.put("role", c.getUser().getRole());
            commentMap.put("content", c.getContent());
            commentMap.put("timestamp", c.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            return commentMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST - Create a new comment on a post
     * POST /api/comments
     * Required: postId, content (in request body as JSON)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createComment(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        // Check if user is logged in
        if (user == null) {
            response.put("success", false);
            response.put("message", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        // Get postId and content from JSON payload
        Long postId = null;
        String content = null;
        
        try {
            Object postIdObj = payload.get("postId");
            if (postIdObj instanceof Number) {
                postId = ((Number) postIdObj).longValue();
            }
            content = (String) payload.get("content");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Invalid request format");
            return ResponseEntity.badRequest().body(response);
        }

        // Check if post exists
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            response.put("success", false);
            response.put("message", "Post not found");
            return ResponseEntity.status(404).body(response);
        }

        // Validate comment content
        if (content == null || content.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Comment cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        if (content.length() > 500) {
            response.put("success", false);
            response.put("message", "Comment exceeds 500 characters");
            return ResponseEntity.badRequest().body(response);
        }

        // Create comment
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);

        Comment savedComment = commentRepository.save(comment);

        // Build response
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", savedComment.getId());
        commentData.put("postId", savedComment.getPost().getId());
        commentData.put("userId", savedComment.getUser().getId());
        commentData.put("author", savedComment.getUser().getUsername());
        commentData.put("avatar", savedComment.getUser().getUsername().charAt(0));
        commentData.put("role", savedComment.getUser().getRole());
        commentData.put("content", savedComment.getContent());
        commentData.put("timestamp", savedComment.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());

        response.put("success", true);
        response.put("message", "Comment created successfully");
        response.put("comment", commentData);
        
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE - Delete a comment
     * DELETE /api/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long commentId,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        // Check if user is logged in
        if (user == null) {
            response.put("success", false);
            response.put("message", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        // Find comment
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            response.put("success", false);
            response.put("message", "Comment not found");
            return ResponseEntity.status(404).body(response);
        }

        // Check permissions (owner or admin)
        if (!comment.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            response.put("success", false);
            response.put("message", "You can only delete your own comments");
            return ResponseEntity.status(403).body(response);
        }

        // Delete comment
        commentRepository.deleteById(commentId);

        response.put("success", true);
        response.put("message", "Comment deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * GET - Get a single comment
     * GET /api/comments/{commentId}
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> getComment(@PathVariable Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        
        if (comment == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Comment not found");
            return ResponseEntity.status(404).body(response);
        }

        Map<String, Object> commentMap = new HashMap<>();
        commentMap.put("id", comment.getId());
        commentMap.put("postId", comment.getPost().getId());
        commentMap.put("userId", comment.getUser().getId());
        commentMap.put("author", comment.getUser().getUsername());
        commentMap.put("avatar", comment.getUser().getUsername().charAt(0));
        commentMap.put("role", comment.getUser().getRole());
        commentMap.put("content", comment.getContent());
        commentMap.put("timestamp", comment.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        
        return ResponseEntity.ok(commentMap);
    }
}

