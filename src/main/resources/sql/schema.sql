-- MySQL dump 10.13  Distrib 5.7.24, for osx11.1 (x86_64)
--
-- Host: localhost    Database: SportX
-- ------------------------------------------------------
-- Server version	8.0.38

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `challenge`
--

DROP TABLE IF EXISTS `challenge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `challenge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `challenge_name` varchar(120) NOT NULL,
  `description` text,
  `total_slots` int NOT NULL DEFAULT '0',
  `joined_slots` int NOT NULL DEFAULT '0',
  `spot_id` bigint NOT NULL,
  `start_time` date NOT NULL,
  `end_time` date NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_challenge_spot` (`spot_id`),
  KEY `idx_challenge_time` (`start_time`,`end_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1004 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `challenge_participation`
--

DROP TABLE IF EXISTS `challenge_participation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `challenge_participation` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `challenge_id` bigint NOT NULL,
  `spot_id` bigint NOT NULL,
  `status` int DEFAULT '1',
  `score` int DEFAULT '0',
  `result` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_challenge` (`user_id`,`challenge_id`),
  UNIQUE KEY `uk_challenge_participation_user_challenge` (`user_id`,`challenge_id`),
  KEY `idx_cp_challenge` (`challenge_id`),
  KEY `idx_cp_spot` (`spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `failed_message`
--

DROP TABLE IF EXISTS `failed_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `failed_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `queue_name` varchar(100) DEFAULT NULL,
  `exchange_name` varchar(100) DEFAULT NULL,
  `routing_key` varchar(100) DEFAULT NULL,
  `payload` text NOT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'NEW',
  `retry_count` int NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `leaderboard_entry`
--

DROP TABLE IF EXISTS `leaderboard_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `leaderboard_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `snapshot_id` bigint NOT NULL,
  `rank` int NOT NULL,
  `target_id` varchar(64) NOT NULL,
  `score` double NOT NULL DEFAULT '0',
  `payload` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_lbe_snapshot` (`snapshot_id`,`rank`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `leaderboard_snapshot`
--

DROP TABLE IF EXISTS `leaderboard_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `leaderboard_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) NOT NULL,
  `period_start` datetime NOT NULL,
  `period_end` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lbs_type_time` (`type`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=7003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(32) NOT NULL,
  `type` varchar(50) NOT NULL,
  `title` varchar(120) NOT NULL,
  `content` varchar(500) NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user` (`user_id`,`is_read`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spot_favorites`
--

DROP TABLE IF EXISTS `spot_favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spot_favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(32) NOT NULL,
  `spot_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav_user_spot` (`user_id`,`spot_id`),
  UNIQUE KEY `uk_spot_favorites_user_spot` (`user_id`,`spot_id`),
  KEY `idx_fav_spot` (`spot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spots`
--

DROP TABLE IF EXISTS `spots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spots` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '场馆ID',
  `name` varchar(100) NOT NULL COMMENT '场馆名称',
  `type` varchar(50) NOT NULL COMMENT '场馆类型',
  `description` text COMMENT '场馆描述',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `region` varchar(50) DEFAULT NULL COMMENT '地区',
  `visit_count` int NOT NULL DEFAULT '0' COMMENT '访问量',
  `rating` double NOT NULL DEFAULT '0' COMMENT '评分',
  `is_open` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否营业',
  `open_time` varchar(100) DEFAULT NULL COMMENT '营业时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_spots_type` (`type`),
  KEY `idx_spots_region` (`region`),
  KEY `idx_spots_rating` (`rating`),
  KEY `idx_spots_is_open` (`is_open`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='场馆表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `id` varchar(32) NOT NULL COMMENT '用户ID',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `password` varchar(255) DEFAULT NULL COMMENT '密码(后续建议哈希)',
  `nickname` varchar(50) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `city` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-20  1:28:10
