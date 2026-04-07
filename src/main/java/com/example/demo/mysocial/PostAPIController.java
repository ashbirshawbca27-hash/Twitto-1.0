package com.example.demo.mysocial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Post;
import com.example.demo.model.Reaction;
import com.example.demo.model.User;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.ReactionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.CommentRepository;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostAPIController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private CommentRepository commentRepository;


    // GET all posts
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<Map<String, Object>>> getFeed(HttpSession session) {
        User user = (User) session.getAttribute("user");
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        Map<Long, Set<String>> userReactionsByPost = new HashMap<>();
        if (user != null) {
            userReactionsByPost = reactionRepository.findByPostIdInAndUserId(postIds, user.getId())
                    .stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getPost().getId(),
                            Collectors.mapping(Reaction::getType, Collectors.toSet())
                    ));
        }
        final Map<Long, Set<String>> finalUserReactionsByPost = userReactionsByPost;

        List<Map<String, Object>> result = posts.stream().map(post -> {
            Map<String, Object> postData = new HashMap<>();
            User author = post.getUser();
            postData.put("id", post.getId());
            postData.put("userId", author == null ? null : author.getId());
            postData.put("username", author == null ? "Unknown" : author.getUsername());
            postData.put("role", author == null ? "USER" : author.getRole());
            postData.put("content", post.getContent());
            postData.put("timestamp", post.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            postData.put("heartCount", post.getHeartCount());
            postData.put("laughCount", post.getLaughCount());
            postData.put("commentCount", commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).size());
            postData.put("userReactions", new ArrayList<>(finalUserReactionsByPost.getOrDefault(post.getId(), Set.of())));
            return postData;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // POST create new post (form submit)
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> createPostFromForm(
            @RequestParam(required = false) String content,
            HttpSession session) {
        return createPostInternal(content, session);
    }

    // POST create new post (JSON)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createPostFromJson(
            @RequestBody(required = false) Map<String, Object> payload,
            HttpSession session) {

        String content = null;
        if (payload != null && payload.get("content") != null) {
            content = String.valueOf(payload.get("content"));
        }

        return createPostInternal(content, session);
    }

    private ResponseEntity<Map<String, Object>> createPostInternal(String content, HttpSession session) {

        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        String safeContent = content == null ? "" : content.trim();
        if (safeContent.isEmpty()) {
            response.put("success", false);
            response.put("message", "Content cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        if (safeContent.length() > 280) {
            response.put("success", false);
            response.put("message", "Content exceeds 280 characters");
            return ResponseEntity.badRequest().body(response);
        }

        Post post = new Post();
        post.setUser(user);
        post.setContent(safeContent);
        
        Post savedPost = postRepository.save(post);

        Map<String, Object> postData = new HashMap<>();
        postData.put("id", savedPost.getId());
        postData.put("userId", user.getId());
        postData.put("username", user.getUsername());
        postData.put("avatar", user.getUsername() == null || user.getUsername().isEmpty()
                ? "?"
                : String.valueOf(Character.toUpperCase(user.getUsername().charAt(0))));
        postData.put("role", user.getRole());
        postData.put("content", savedPost.getContent());
        postData.put("timestamp", savedPost.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        postData.put("heartCount", savedPost.getHeartCount());
        postData.put("laughCount", savedPost.getLaughCount());

        response.put("success", true);
        response.put("message", "Post created successfully");
        response.put("post", postData);
        return ResponseEntity.ok(response);
    }

    // DELETE post
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePost(
            @PathVariable Long id,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            response.put("success", false);
            response.put("message", "Post not found");
            return ResponseEntity.status(404).body(response);
        }

        if (!post.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            response.put("success", false);
            response.put("message", "You can only delete your own posts");
            return ResponseEntity.status(403).body(response);
        }


        reactionRepository.deleteByPostId(id);
        postRepository.deleteById(id);
        response.put("success", true);
        response.put("message", "Post deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reactions")
    public ResponseEntity<Map<String, Object>> toggleReaction(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        String type = payload == null || payload.get("type") == null ? "" : String.valueOf(payload.get("type")).trim().toLowerCase();
        if (!"heart".equals(type) && !"laugh".equals(type)) {
            response.put("success", false);
            response.put("message", "Invalid reaction type");
            return ResponseEntity.badRequest().body(response);
        }

        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            response.put("success", false);
            response.put("message", "Post not found");
            return ResponseEntity.status(404).body(response);
        }

        Reaction existing = reactionRepository.findByPostIdAndUserIdAndType(id, user.getId(), type);
        if (existing != null) {
            reactionRepository.delete(existing);
        } else {
            Reaction reaction = new Reaction();
            reaction.setPost(post);
            reaction.setUser(user);
            reaction.setType(type);
            reactionRepository.save(reaction);
        }

        int heartCount = (int) reactionRepository.countByPostIdAndType(id, "heart");
        int laughCount = (int) reactionRepository.countByPostIdAndType(id, "laugh");
        post.setHeartCount(heartCount);
        post.setLaughCount(laughCount);
        postRepository.save(post);

        List<String> userReactions = reactionRepository.findByPostIdAndUserId(id, user.getId())
                .stream()
                .map(Reaction::getType)
                .toList();

        response.put("success", true);
        response.put("postId", id);
        response.put("heartCount", heartCount);
        response.put("laughCount", laughCount);
        response.put("userReactions", userReactions);
        return ResponseEntity.ok(response);
    }
}
