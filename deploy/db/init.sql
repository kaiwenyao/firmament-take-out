-- MySQL dump 10.13  Distrib 8.4.11, for Linux (aarch64)
--
-- Host: localhost    Database: firmament_take_out
-- ------------------------------------------------------
-- Server version	8.4.11

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `address_book`
--

DROP TABLE IF EXISTS `address_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `consignee` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '收货人',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '详细地址',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2006414661865545731 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='地址簿';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `address_book`
--

LOCK TABLES `address_book` WRITE;
/*!40000 ALTER TABLE `address_book` DISABLE KEYS */;
INSERT INTO `address_book` VALUES (2004154563399020545,2004144106810408962,'Demo User One','1','13800000005','','Demo Province','','Demo City','','Demo District','Demo Street No. 1','1',0),(2006414661865545730,2004144106810408962,'Demo User Two','1','13800000006','','','','','','','Demo Address Two',NULL,1);
/*!40000 ALTER TABLE `address_book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` int DEFAULT NULL COMMENT '类型   1 菜品分类 2 套餐分类',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '顺序',
  `status` int DEFAULT NULL COMMENT '分类状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_category_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2005594419828424706 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='菜品及套餐分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (11,1,'Beverages',10,1,'2022-06-09 22:09:18','2022-06-09 22:09:18',1,1),(12,1,'Traditional Staples',9,1,'2022-06-09 22:09:32','2022-06-09 22:18:53',1,1),(13,2,'Popular Combos',12,1,'2022-06-09 22:11:38','2022-06-10 11:04:40',1,1),(15,2,'Business Set Menu',13,1,'2022-06-09 22:14:10','2022-06-10 11:04:48',1,1),(16,1,'Shu-style Grilled Fish',5,1,'2022-06-09 22:15:37','2025-12-22 16:58:51',1,1),(17,1,'Shu-style Bullfrog',4,1,'2022-06-09 22:16:14','2025-12-22 16:59:00',1,1),(18,1,'Specialty Steamed Dishes',6,1,'2022-06-09 22:17:42','2025-12-23 01:58:12',1,1),(19,1,'Fresh Vegetables',7,1,'2022-06-09 22:18:12','2022-06-09 22:18:28',1,1),(20,1,'Pickled Cabbage Fish',8,1,'2022-06-09 22:22:29','2022-06-09 22:23:45',1,1),(21,1,'Soups',11,1,'2022-06-10 10:51:47','2022-06-10 10:51:47',1,1);
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish`
--

DROP TABLE IF EXISTS `dish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '菜品名称',
  `category_id` bigint NOT NULL COMMENT '菜品分类id',
  `price` decimal(10,2) DEFAULT NULL COMMENT '菜品价格',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '描述信息',
  `status` int DEFAULT '1' COMMENT '0 停售 1 起售',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dish_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='菜品';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish`
--

LOCK TABLES `dish` WRITE;
/*!40000 ALTER TABLE `dish` DISABLE KEYS */;
INSERT INTO `dish` VALUES (46,'Wong Lo Kat Herbal Tea',11,6.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/f5715f8c-f407-4269-baac-6d0e2c865a14.png','',1,'2022-06-09 22:40:47','2025-12-28 03:17:49',1,1),(47,'Arctic Ocean Soda',11,4.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/f2527cff-52b8-485c-bf33-d9c0e9b688ea.png','Classic childhood flavor',1,'2022-06-10 09:18:49','2025-12-28 03:17:26',1,1),(48,'Snow Beer',11,4.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/701abc39-56d0-4f79-abe6-3d41ac52c2ed.png','',1,'2022-06-10 09:22:54','2025-12-28 03:16:56',1,1),(49,'Steamed Rice',12,2.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/7a7ec44e-da02-429b-a5f5-a40ff5a2b42d.png','Premium Wuchang rice',1,'2022-06-10 09:30:17','2025-12-28 03:16:06',1,1),(50,'Steamed Bun',12,1.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/214b96a7-472a-428c-aac9-3102db12e78e.png','Quality flour',1,'2022-06-10 09:34:28','2025-12-28 03:15:11',1,1),(51,'Classic Pickled Fish with Cabbag',20,56.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/ae2d970e-ee18-48f3-b54d-70141e414028.png','Ingredients: broth, grass carp, pickled cabbage',1,'2022-06-10 09:40:51','2025-12-28 03:14:28',1,1),(52,'Classic Pickled Catfish',20,66.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/3f9353fe-c802-4806-8e10-ae3f1af26efb.png','Ingredients: pickled greens, river catfish',1,'2022-06-10 09:46:02','2025-12-28 03:13:50',1,1),(53,'Shu-style Boiled Grass Carp',20,38.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/99706603-1bf0-4c41-84a7-a2bf15296c55.png','Ingredients: grass carp, broth',1,'2022-06-10 09:48:37','2025-12-28 03:13:32',1,1),(54,'Stir-fried Bok Choy',19,18.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/adabe877-13bb-48f6-8ec4-8d2aaeaf0146.png','Ingredients: bok choy',1,'2022-06-10 09:51:46','2025-12-28 03:12:40',1,1),(55,'Baby Cabbage with Garlic',19,18.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/8ea1aa1e-c1ae-49c0-8257-21f68d559085.png','Ingredients: garlic, baby cabbage',1,'2022-06-10 09:53:37','2025-12-28 03:12:09',1,1),(56,'Stir-fried Broccoli',19,18.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/ef60adbe-b110-4d5f-89de-6f5063445684.png','Ingredients: broccoli',1,'2022-06-10 09:55:44','2025-12-28 03:11:31',1,1),(57,'Stir-fried Cabbage',19,18.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/35687014-d346-403f-b329-acdcc3c4d33f.png','Ingredients: cabbage',1,'2022-06-10 09:58:35','2025-12-30 14:16:57',1,1),(58,'Steamed Sea Bass',18,98.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/6869839e-9801-4ba1-a5ef-5bdef971bf9b.png','Ingredients: sea bass',1,'2022-06-10 10:12:28','2025-12-28 03:10:27',1,1),(59,'Dongpo Pork Hock',18,138.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/5c6b5fd7-581a-474d-b55a-98791346b58e.png','Ingredients: pork hock',1,'2022-06-10 10:24:03','2025-12-28 03:09:44',1,1),(60,'Steamed Pork with Preserved Gree',18,58.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/7b6ec1d4-ea7c-487d-8252-91f26792d15b.png','Ingredients: pork, preserved mustard greens',1,'2022-06-10 10:26:03','2025-12-28 03:09:16',1,1),(61,'Steamed Fish Head with Chilies',18,66.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/d956de80-dc77-4202-8bf5-51a1527277ce.png','Ingredients: silver carp head, chopped chilies',1,'2022-06-10 10:28:54','2025-12-28 03:08:24',1,1),(62,'Golden Broth Bullfrog',17,88.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/73859397-5211-4bad-9a8f-19082c76cdd3.png','Ingredients: fresh bullfrog, pickled greens',1,'2022-06-10 10:33:05','2025-12-28 03:07:40',1,1),(63,'Dry Pot Bullfrog',17,88.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/b6780917-c70c-4355-8b52-7d5eb6701b96.png','Ingredients: bullfrog, lotus root, bamboo shoots',1,'2022-06-10 10:35:40','2025-12-28 03:06:56',1,1),(64,'Spicy Bullfrog',17,88.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/01df828e-2ca6-4555-ba8e-f4b48f177cfd.png','Ingredients: bullfrog, sponge gourd, soy sprouts',1,'2022-06-10 10:37:52','2025-12-28 03:06:35',1,1),(65,'Grass Carp 2 lb',16,68.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/59b70692-511e-4190-9770-d64a226e0286.png','Ingredients: grass carp, soy sprouts, lotus root',1,'2022-06-10 10:41:08','2025-12-28 03:05:57',1,1),(66,'River Catfish 2 lb',16,119.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/3ef567de-1d0b-4259-a17a-5ece63c7c971.png','Ingredients: river catfish, soy sprouts, lotus root',1,'2022-06-10 10:42:42','2025-12-28 03:04:53',1,1),(67,'Qingjiang Fish 2 lb',16,72.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/7067006f-82c8-4c32-b7bf-8b679001ee03.png','Ingredients: channel catfish, soy sprouts, lotus root',1,'2022-06-10 10:43:56','2025-12-31 12:11:30',1,1),(68,'Egg Drop Soup',21,111.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/9c0e6eb8-2b92-4b5a-ba88-1b259bdbc26e.png','Ingredients: egg, seaweed',1,'2022-06-10 10:54:25','2025-12-28 03:02:14',1,1),(69,'Oyster Mushroom Tofu Soup',21,6.00,'https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/aeb8f962-3b01-489d-8f27-28fd2230916d.png','Ingredients: tofu, oyster mushroom',1,'2022-06-10 10:55:02','2025-12-28 22:10:53',1,1);
/*!40000 ALTER TABLE `dish` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_flavor`
--

DROP TABLE IF EXISTS `dish_flavor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_flavor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '菜品',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '口味名称',
  `value` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '口味数据list',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=136 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='菜品口味关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_flavor`
--

LOCK TABLES `dish_flavor` WRITE;
/*!40000 ALTER TABLE `dish_flavor` DISABLE KEYS */;
INSERT INTO `dish_flavor` VALUES (40,10,'Sweetness','[\"No Sugar\",\"Light\",\"Half\",\"Extra\",\"Full\"]'),(41,7,'Dietary','[\"No Scallions\",\"No Garlic\",\"No Cilantro\",\"No Spice\"]'),(42,7,'Temperature','[\"Hot\",\"Room Temp\",\"No Ice\",\"Light Ice\",\"Extra Ice\"]'),(45,6,'Dietary','[\"No Scallions\",\"No Garlic\",\"No Cilantro\",\"No Spice\"]'),(46,6,'Spice Level','[\"None\",\"Mild\",\"Medium\",\"Extra Hot\"]'),(47,5,'Spice Level','[\"None\",\"Mild\",\"Medium\",\"Extra Hot\"]'),(48,5,'Sweetness','[\"No Sugar\",\"Light\",\"Half\",\"Extra\",\"Full\"]'),(50,4,'Sweetness','[\"No Sugar\",\"Light\",\"Half\",\"Extra\",\"Full\"]'),(123,69,'Sweetness','[\"No Sugar\",\"Light\",\"Half\",\"Extra\",\"Full\"]'),(128,69,'Temperature','[\"No Ice\",\"Extra Ice\"]'),(133,69,'Spice Level','[\"None\",\"Mild\",\"Medium\",\"Extra Hot\"]'),(134,67,'Spice Level','[\"None\",\"Mild\",\"Medium\",\"Extra Hot\"]'),(135,67,'Temperature','[\"Hot\",\"Room Temp\",\"No Ice\",\"Light Ice\",\"Extra Ice\"]');
/*!40000 ALTER TABLE `dish_flavor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '用户名',
  `password` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '密码',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '身份证号',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=82 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='员工信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee`
--

LOCK TABLES `employee` WRITE;
/*!40000 ALTER TABLE `employee` DISABLE KEYS */;
INSERT INTO `employee` VALUES (1,'Administrator','admin','{BCRYPT}$2a$10$rfWl8eF4t/I/K4xgc4uL4.dhBSZw.VsoEChZHImpNHE7Rq2SU5yhe','13800000001','1','110101199001010001',1,'2022-02-15 15:51:20','2025-12-28 21:04:35',10,1),(20,'Rebecca Raynor','DennisManteMD','e10adc3949ba59abbe56e057f20f883e','13800000002','0','110101199001010002',1,'2025-12-22 14:05:25','2025-12-29 10:59:24',1,1),(21,'Wang Wu','demo_staff','e10adc3949ba59abbe56e057f20f883e','13800000003','1','110101199001010003',1,'2025-12-22 14:06:32','2025-12-27 18:14:41',20,1),(81,'Demo Employee','demo_employee','{BCRYPT}$2a$10$rfWl8eF4t/I/K4xgc4uL4.dhBSZw.VsoEChZHImpNHE7Rq2SU5yhe','13800000004','0','110101199001010004',1,'2025-12-28 15:54:35','2025-12-29 10:59:32',1,1);
/*!40000 ALTER TABLE `employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_detail`
--

DROP TABLE IF EXISTS `order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '名字',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2006431750630215683 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='订单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_detail`
--

LOCK TABLES `order_detail` WRITE;
/*!40000 ALTER TABLE `order_detail` DISABLE KEYS */;
INSERT INTO `order_detail` VALUES (2004208138338721793,'Golden Broth Bullfrog','https://sky-itcast.oss-cn-beijing.aliyuncs.com/7694a5d8-7938-4e9d-8b9e-2075983a2e38.png',2004208137571164162,62,NULL,NULL,3,88.00),(2004209731331219458,'Golden Broth Bullfrog','https://sky-itcast.oss-cn-beijing.aliyuncs.com/7694a5d8-7938-4e9d-8b9e-2075983a2e38.png',2004209730915983361,62,NULL,NULL,3,88.00),(2004209731335413761,'Dongpo Pork Hock','https://sky-itcast.oss-cn-beijing.aliyuncs.com/a80a4b8c-c93e-4f43-ac8a-856b0d5cc451.png',2004209730915983361,59,NULL,NULL,1,138.00),(2004211447082586113,'Grass Carp 2 lb','https://sky-itcast.oss-cn-beijing.aliyuncs.com/b544d3ba-a1ae-4d20-a860-81cb5dec9e03.png',2004211446650572801,65,NULL,'Extra Hot',1,68.00),(2004211447086780418,'River Catfish 2 lb','https://sky-itcast.oss-cn-beijing.aliyuncs.com/a101a1e9-8f8b-47b2-afa4-1abd47ea0a87.png',2004211446650572801,66,NULL,'Extra Hot',1,119.00),(2004211447090974722,'Qingjiang Fish 2 lb','https://sky-itcast.oss-cn-beijing.aliyuncs.com/8cfcc576-4b66-4a09-ac68-ad5b273c2590.png',2004211446650572801,67,NULL,'Extra Hot',1,72.00),(2004571855484108802,'Steamed Sea Bass','https://sky-itcast.oss-cn-beijing.aliyuncs.com/c18b5c67-3b71-466c-a75a-e63c6449f21c.png',2004571855022735362,58,NULL,NULL,1,98.00),(2004571855488303105,'Dongpo Pork Hock','https://sky-itcast.oss-cn-beijing.aliyuncs.com/a80a4b8c-c93e-4f43-ac8a-856b0d5cc451.png',2004571855022735362,59,NULL,NULL,1,138.00),(2004686227275612162,'Golden Broth Bullfrog','https://sky-itcast.oss-cn-beijing.aliyuncs.com/7694a5d8-7938-4e9d-8b9e-2075983a2e38.png',2004686226847793154,62,NULL,NULL,10,88.00),(2005014245399392257,'Dongpo Pork Hock','https://sky-itcast.oss-cn-beijing.aliyuncs.com/a80a4b8c-c93e-4f43-ac8a-856b0d5cc451.png',2005014244879298561,59,NULL,NULL,10,138.00),(2005016043380076546,'Egg Drop Soup','https://sky-itcast.oss-cn-beijing.aliyuncs.com/c09a0ee8-9d19-428d-81b9-746221824113.png',2005016042960646145,68,NULL,NULL,9,111.00),(2005016043384270850,'Egg Drop Soup','https://sky-itcast.oss-cn-beijing.aliyuncs.com/c09a0ee8-9d19-428d-81b9-746221824113.png',2005016042960646145,68,NULL,NULL,1,111.00),(2005120569864937474,'Baby Cabbage with Garlic','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/8ea1aa1e-c1ae-49c0-8257-21f68d559085.png',2005120569441312770,55,NULL,NULL,8,18.00),(2005400800685232130,'Steamed Rice','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/7a7ec44e-da02-429b-a5f5-a40ff5a2b42d.png',2005400800261607426,49,NULL,NULL,10,2.00),(2006079962714574850,'Golden Broth Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/73859397-5211-4bad-9a8f-19082c76cdd3.png',2006079961926045698,62,NULL,'',1,88.00),(2006079962722963458,'Dry Pot Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/b6780917-c70c-4355-8b52-7d5eb6701b96.png',2006079961926045698,63,NULL,'',1,88.00),(2006079962727157761,'Spicy Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/01df828e-2ca6-4555-ba8e-f4b48f177cfd.png',2006079961926045698,64,NULL,'',1,88.00),(2006083511116730370,'Stir-fried Bok Choy','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/adabe877-13bb-48f6-8ec4-8d2aaeaf0146.png',2006083510655356930,54,NULL,'',10,18.00),(2006129209065443329,'Golden Broth Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/73859397-5211-4bad-9a8f-19082c76cdd3.png',2006129208612458498,62,NULL,'',5,88.00),(2006337465314537474,'Qingjiang Fish 2 lb','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/7067006f-82c8-4c32-b7bf-8b679001ee03.png',2006337464526008321,67,NULL,'Spice: Extra Hot, Ice: Extra',1,72.00),(2006338598493843457,'Qingjiang Fish 2 lb','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/7067006f-82c8-4c32-b7bf-8b679001ee03.png',2006338598015692801,67,NULL,'No Ice, Medium',1,72.00),(2006377202758819842,'Oyster Mushroom Tofu Soup','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/aeb8f962-3b01-489d-8f27-28fd2230916d.png',2006377201882210305,69,NULL,'Sweet: Full, Ice: Extra, Spice: Extra Hot',1,6.00),(2006414683722063873,'Golden Broth Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/73859397-5211-4bad-9a8f-19082c76cdd3.png',2006414683256496129,62,NULL,'',1,88.00),(2006416522559463426,'Golden Broth Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/73859397-5211-4bad-9a8f-19082c76cdd3.png',2006416522093895682,62,NULL,'',1,88.00),(2006417571080937473,'Dry Pot Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/b6780917-c70c-4355-8b52-7d5eb6701b96.png',2006417570636341250,63,NULL,'',10,88.00),(2006423870619308033,'123','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/9db75514-2e7e-4ecc-b521-3aff6d71d445.jpg',2006423870149545986,NULL,4,NULL,1,12111.00),(2006425505064079362,'jknkj','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/094333d4-7c5c-4def-a339-b4aa13ac66c4.png',2006425504606900226,NULL,3,NULL,3,1111.00),(2006428776826720258,'jknkj','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/094333d4-7c5c-4def-a339-b4aa13ac66c4.png',2006428776361152513,NULL,3,NULL,1,1111.00),(2006428776835108865,'Spicy Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/01df828e-2ca6-4555-ba8e-f4b48f177cfd.png',2006428776361152513,64,NULL,'',10,88.00),(2006431750626021377,'Dry Pot Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/b6780917-c70c-4355-8b52-7d5eb6701b96.png',2006431750147870722,63,NULL,'',1,88.00),(2006431750630215681,'River Catfish 2 lb','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/3ef567de-1d0b-4259-a17a-5ece63c7c971.png',2006431750147870722,66,NULL,'',1,119.00),(2006431750630215682,'Qingjiang Fish 2 lb','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/7067006f-82c8-4c32-b7bf-8b679001ee03.png',2006431750147870722,67,NULL,'Spice: Extra Hot, Ice: Extra',1,72.00);
/*!40000 ALTER TABLE `order_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '订单号',
  `status` int NOT NULL DEFAULT '1' COMMENT '订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款',
  `user_id` bigint NOT NULL COMMENT '下单用户',
  `address_book_id` bigint NOT NULL COMMENT '地址id',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '结账时间',
  `pay_method` int NOT NULL DEFAULT '1' COMMENT '支付方式 1微信,2支付宝',
  `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态 0未支付 1已支付 2退款',
  `amount` decimal(10,2) NOT NULL COMMENT '实收金额',
  `remark` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '手机号',
  `address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '地址',
  `user_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户名称',
  `consignee` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '收货人',
  `cancel_reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '订单取消原因',
  `rejection_reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '订单拒绝原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '订单取消时间',
  `estimated_delivery_time` datetime DEFAULT NULL COMMENT '预计送达时间',
  `delivery_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '配送状态  1立即送出  0选择具体时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '送达时间',
  `pack_amount` int DEFAULT NULL COMMENT '打包费',
  `tableware_number` int DEFAULT NULL COMMENT '餐具数量',
  `tableware_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '餐具数量状态  1按餐量提供  0选择具体数量',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2006431750147870723 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (2004208137571164162,'202512251510318962',6,2004144106810408962,2004154563399020545,'2025-12-25 15:10:31','2025-12-25 15:10:33',1,2,273.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One','High order volume; temporarily unable to accept orders',NULL,'2025-12-25 15:14:27','2025-12-25 16:10:00',0,NULL,0,0,0),(2004209730915983361,'202512251516508962',5,2004144106810408962,2004154563399020545,'2025-12-25 15:16:51','2025-12-25 15:16:53',1,1,412.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,NULL,NULL,'2025-12-25 16:16:00',0,'2025-12-25 15:17:15',0,0,0),(2004211446650572801,'202512251523398962',5,2004144106810408962,2004154563399020545,'2025-12-25 15:23:40','2025-12-25 15:23:42',1,1,268.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,NULL,NULL,'2025-12-25 16:23:00',0,'2025-12-26 14:06:04',0,0,0),(2004571855022735362,'202512261515488962',5,2004144106810408962,2004154563399020545,'2025-12-26 15:15:48','2025-12-26 15:15:50',1,1,244.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,NULL,NULL,'2025-12-26 16:15:00',0,'2025-12-26 15:16:13',0,0,0),(2004686226847793154,'202512262250168962',5,2004144106810408962,2004154563399020545,'2025-12-26 22:50:16','2025-12-26 22:50:18',1,1,896.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,NULL,NULL,'2025-12-26 23:50:00',0,'2025-12-31 18:17:19',0,0,0),(2005014244879298561,'202512272033418962',3,2004144106810408962,2004154563399020545,'2025-12-27 20:33:42','2025-12-27 20:33:48',1,1,1396.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,NULL,NULL,'2025-12-27 21:33:00',0,NULL,0,0,0),(2005016042960646145,'202512272040508962',3,2004144106810408962,2004154563399020545,'2025-12-27 20:40:51','2025-12-27 20:40:52',1,1,1126.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,NULL,NULL,'2025-12-27 21:40:00',0,NULL,0,0,0),(2005120569441312770,'202512280336118962',6,2004144106810408962,2004154563399020545,'2025-12-28 03:36:12','2025-12-28 03:36:13',1,2,158.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,'1','2025-12-31 18:16:37','2025-12-28 04:36:00',0,NULL,0,0,0),(2005400800261607426,'202512282209438962',6,2004144106810408962,2004154563399020545,'2025-12-28 22:09:44','2025-12-28 22:09:48',1,2,36.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,'1','2025-12-31 18:16:41','2025-12-28 23:09:00',0,NULL,0,0,0),(2006079961926045698,'202512301908288962',6,2004144106810408962,2004154563399020545,'2025-12-30 19:08:29','2025-12-30 19:08:29',1,2,264.00,NULL,'13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,'1','2025-12-31 18:16:45',NULL,1,NULL,0,0,1),(2006083510655356930,'202512301922348962',6,2004144106810408962,2004154563399020545,'2025-12-30 19:22:35','2025-12-30 19:22:35',1,2,180.00,NULL,'13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,'1','2025-12-31 18:16:49',NULL,1,NULL,0,0,1),(2006129208612458498,'202512302224108962',6,2004144106810408962,2004154563399020545,'2025-12-30 22:24:10','2025-12-30 22:24:11',1,2,440.00,NULL,'13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,'1','2025-12-31 18:16:52',NULL,1,NULL,0,0,1),(2006337464526008321,'202512311211428962',6,2004144106810408962,2004154563399020545,'2025-12-31 12:11:42','2025-12-31 12:11:43',1,2,72.00,NULL,'13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,'1','2025-12-31 18:16:55',NULL,1,NULL,0,0,1),(2006338598015692801,'202512311216128962',6,2004144106810408962,2004154563399020545,'2025-12-31 12:16:12','2025-12-31 12:16:19',1,2,79.00,'','13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,'1','2025-12-31 18:16:58','2025-12-31 13:16:00',0,NULL,0,0,0),(2006377201882210305,'202512311449368962',6,2004144106810408962,2004154563399020545,'2025-12-31 14:49:36','2025-12-31 14:49:37',1,2,6.00,NULL,'13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,'11','2025-12-31 18:17:01',NULL,1,NULL,0,0,1),(2006414683256496129,'202512311718328962',2,2004144106810408962,2006414661865545730,'2025-12-31 17:18:32','2025-12-31 17:21:42',1,1,88.00,NULL,'13800000006','Demo Address Two',NULL,'Demo User Two',NULL,NULL,NULL,NULL,1,NULL,0,0,1),(2006416522093895682,'202512311725508962',2,2004144106810408962,2006414661865545730,'2025-12-31 17:25:51','2025-12-31 17:28:06',1,1,88.00,NULL,'13800000006','Demo Address Two',NULL,'Demo User Two',NULL,NULL,NULL,NULL,1,NULL,0,0,1),(2006417570636341250,'202512311730008962',6,2004144106810408962,2006414661865545730,'2025-12-31 17:30:01',NULL,1,0,880.00,NULL,'13800000006','Demo Address Two',NULL,'Demo User Two','Order timed out; auto-cancelled',NULL,'2025-12-31 17:46:00',NULL,1,NULL,0,0,1),(2006423870149545986,'202512311755028962',6,2004144106810408962,2006414661865545730,'2025-12-31 17:55:03','2025-12-31 17:55:04',1,2,12111.00,NULL,'13800000006','Demo Address Two',NULL,'Demo User Two','User cancelled',NULL,'2025-12-31 18:43:43',NULL,1,NULL,0,0,1),(2006425504606900226,'202512311801328962',6,2004144106810408962,2006414661865545730,'2025-12-31 18:01:33',NULL,1,0,3333.00,NULL,'13800000006','Demo Address Two',NULL,'Demo User Two','Order timed out; auto-cancelled',NULL,'2025-12-31 18:17:00',NULL,1,NULL,0,0,1),(2006428776361152513,'202512311814328962',5,2004144106810408962,2004154563399020545,'2025-12-31 18:14:33','2025-12-31 18:15:37',1,1,1991.00,NULL,'13800000005','Demo Province, Demo City, Demo District, Demo Street No. 1',NULL,'Demo User One',NULL,NULL,NULL,NULL,1,'2025-12-31 18:17:17',0,0,1),(2006431750147870722,'202512311826218962',6,2004144106810408962,2006414661865545730,'2025-12-31 18:26:22','2025-12-31 18:26:29',1,2,279.00,NULL,'13800000006','Demo Address Two',NULL,'Demo User Two','User cancelled',NULL,'2025-12-31 18:42:33',NULL,1,NULL,0,0,1);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `setmeal`
--

DROP TABLE IF EXISTS `setmeal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '菜品分类id',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL COMMENT '套餐价格',
  `status` int DEFAULT '1' COMMENT '售卖状态 0:停售 1:起售',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '描述信息',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_setmeal_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='套餐';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `setmeal`
--

LOCK TABLES `setmeal` WRITE;
/*!40000 ALTER TABLE `setmeal` DISABLE KEYS */;
INSERT INTO `setmeal` VALUES (3,13,'jknkj',1111.00,1,'','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/094333d4-7c5c-4def-a339-b4aa13ac66c4.png','2025-12-24 12:27:22','2025-12-29 10:58:22',1,1),(4,15,'123',12111.00,1,'','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/9db75514-2e7e-4ecc-b521-3aff6d71d445.jpg','2025-12-29 09:43:11','2025-12-30 13:44:15',1,1);
/*!40000 ALTER TABLE `setmeal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `setmeal_dish`
--

DROP TABLE IF EXISTS `setmeal_dish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal_dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '菜品名称 （冗余字段）',
  `price` decimal(10,2) DEFAULT NULL COMMENT '菜品单价（冗余字段）',
  `copies` int DEFAULT NULL COMMENT '菜品份数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2005575314752733187 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='套餐菜品关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `setmeal_dish`
--

LOCK TABLES `setmeal_dish` WRITE;
/*!40000 ALTER TABLE `setmeal_dish` DISABLE KEYS */;
INSERT INTO `setmeal_dish` VALUES (2003798634652590082,2003798633998278657,49,'Steamed Rice',2.00,1),(2003798634656784385,2003798633998278657,54,'Stir-fried Bok Choy',18.00,1),(2003798634656784386,2003798633998278657,66,'River Catfish 2 lb',119.00,1),(2003798634656784387,2003798633998278657,64,'Spicy Bullfrog',88.00,1),(2003804692364226562,3,62,'Golden Broth Bullfrog',88.00,1),(2003804692372615170,3,64,'Spicy Bullfrog',88.00,1),(2003804692372615171,3,63,'Dry Pot Bullfrog',88.00,1),(2005575314744344577,4,62,'Golden Broth Bullfrog',88.00,1),(2005575314752733185,4,63,'Dry Pot Bullfrog',88.00,1),(2005575314752733186,4,64,'Spicy Bullfrog',88.00,1);
/*!40000 ALTER TABLE `setmeal_dish` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shopping_cart`
--

DROP TABLE IF EXISTS `shopping_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '商品名称',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `user_id` bigint NOT NULL COMMENT '主键',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2006432592850653187 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='购物车';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shopping_cart`
--

LOCK TABLES `shopping_cart` WRITE;
/*!40000 ALTER TABLE `shopping_cart` DISABLE KEYS */;
INSERT INTO `shopping_cart` VALUES (2006432592850653186,'Spicy Bullfrog','https://itheima-java-web-ai-bucket.oss-eu-central-1.aliyuncs.com/01df828e-2ca6-4555-ba8e-f4b48f177cfd.png',2004144106810408962,64,NULL,'',1,88.00,'2025-12-31 18:29:43');
/*!40000 ALTER TABLE `shopping_cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '微信用户唯一标识',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '头像',
  `create_time` datetime DEFAULT NULL,
  `password` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2004144106810408964 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (2004144106810408962,'ofake00000000000000000000000001',NULL,'13800000005',NULL,NULL,NULL,'2025-12-25 10:56:05','{BCRYPT}$2a$10$rfWl8eF4t/I/K4xgc4uL4.dhBSZw.VsoEChZHImpNHE7Rq2SU5yhe'),(2004144106810408963,NULL,NULL,'13800000006',NULL,NULL,NULL,NULL,'{BCRYPT}$2a$10$rfWl8eF4t/I/K4xgc4uL4.dhBSZw.VsoEChZHImpNHE7Rq2SU5yhe');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed
