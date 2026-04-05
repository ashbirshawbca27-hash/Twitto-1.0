-- Fix is_online column type from VARCHAR to TINYINT
-- This migration fixes the "Data too long for column 'is_online'" error

-- First, check if column exists and alter it
ALTER TABLE users MODIFY COLUMN is_online TINYINT(1) DEFAULT 0;

