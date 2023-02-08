/*
 Navicat Premium Data Transfer

 Source Server         : 本地环境数据库
 Source Server Type    : MySQL
 Source Server Version : 50728
 Source Host           : 127.0.0.1:3306
 Source Schema         : order_db

 Target Server Type    : MySQL
 Target Server Version : 50728
 File Encoding         : 65001

 Date: 08/02/2023 17:48:09
*/

CREATE TABLE order_db;

USE order_db;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_order_1
-- ----------------------------
DROP TABLE IF EXISTS `t_order_1`;
CREATE TABLE `t_order_1` (
  `order_id` bigint(20) NOT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_order_1
-- ----------------------------
BEGIN;
INSERT INTO `t_order_1` VALUES (500, 100.00, 1000, 'SUCCESS');
INSERT INTO `t_order_1` VALUES (620244932191322112, 22.50, 2, 'SUCCESS');
INSERT INTO `t_order_1` VALUES (620244932388454400, 24.50, 2, 'SUCCESS');
INSERT INTO `t_order_1` VALUES (620244932535255040, 26.50, 2, 'SUCCESS');
INSERT INTO `t_order_1` VALUES (620244932744970240, 28.50, 2, 'SUCCESS');
INSERT INTO `t_order_1` VALUES (620244932891770880, 30.50, 2, 'SUCCESS');
INSERT INTO `t_order_1` VALUES (620244933034377216, 32.50, 2, 'SUCCESS');
INSERT INTO `t_order_1` VALUES (620244933168594944, 34.50, 2, 'SUCCESS');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
