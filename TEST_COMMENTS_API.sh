#!/bin/bash

# Comment API Test Script
# This script helps test the comment endpoints after the fixes

API_URL="http://localhost:8080/api/comments"

echo "=== Comment API Test Script ==="
echo ""

# Test 1: Get comments for post ID 1
echo "Test 1: Get comments for post 1"
echo "Command: curl -X GET $API_URL/post/1"
echo ""

# Test 2: Create a comment (requires user to be logged in via session)
echo "Test 2: Create a comment on post 1"
echo "Command: curl -X POST $API_URL \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"postId\": 1, \"content\": \"Great post!\"}' \\"
echo "  -b 'JSESSIONID=your_session_id'"
echo ""

# Test 3: Get a specific comment
echo "Test 3: Get comment with ID 1"
echo "Command: curl -X GET $API_URL/1"
echo ""

# Test 4: Delete a comment (requires user to be logged in and own the comment)
echo "Test 4: Delete comment with ID 1"
echo "Command: curl -X DELETE $API_URL/1 \\"
echo "  -b 'JSESSIONID=your_session_id'"
echo ""

echo "=== Key Points ==="
echo "✓ All comment endpoints now use JSON request bodies"
echo "✓ CORS is enabled for cross-origin requests"
echo "✓ User must be logged in (session attribute 'user' must be set)"
echo "✓ Comments are tied to the logged-in user"
echo "✓ Only comment owners or admins can delete comments"

