-- GEA Database Baseline Schema
-- Generado con: mysqldump --no-data --user=root --password=1234 --host=localhost gea_db
-- Fecha: 2026-06-02
--
-- NOTA: Con baseline-on-migrate=true y baseline-version=1, Flyway NO ejecuta
-- este SQL en bases de datos que ya tienen tablas — solo registra V1 como aplicada.
-- Las migraciones futuras se agregan como V2__, V3__, etc.
--
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: gea_db
-- ------------------------------------------------------
-- Server version	8.0.45

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
-- Table structure for table `archivos_adjuntos`
--

DROP TABLE IF EXISTS `archivos_adjuntos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `archivos_adjuntos` (
  `publico` bit(1) NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `id_archivo_adjunto` bigint NOT NULL AUTO_INCREMENT,
  `tamano` bigint NOT NULL,
  `content_type` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_acceso` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_almacenado` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_original` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_archivo_adjunto`),
  UNIQUE KEY `UK8iisd42d1rs3axwcpkw5s047u` (`token_acceso`),
  UNIQUE KEY `UKhve4rv40his64vvmnk9p68871` (`nombre_almacenado`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_revision_info`
--

DROP TABLE IF EXISTS `audit_revision_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_revision_info` (
  `rev` int NOT NULL,
  `revtstmp` bigint DEFAULT NULL,
  `ip_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre_usuario` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_revision_info_seq`
--

DROP TABLE IF EXISTS `audit_revision_info_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_revision_info_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_revisions`
--

DROP TABLE IF EXISTS `audit_revisions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_revisions` (
  `rev_id` int NOT NULL AUTO_INCREMENT,
  `ip_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre_usuario` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rev_timestamp` bigint DEFAULT NULL,
  PRIMARY KEY (`rev_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dispositivos_usuario`
--

DROP TABLE IF EXISTS `dispositivos_usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dispositivos_usuario` (
  `id_dispositivo` bigint NOT NULL AUTO_INCREMENT,
  `fecha_actualizacion` datetime(6) DEFAULT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `usuario_actualizacion` varchar(255) DEFAULT NULL,
  `usuario_creacion` varchar(255) DEFAULT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `token` varchar(500) NOT NULL,
  `id_usuario` bigint NOT NULL,
  PRIMARY KEY (`id_dispositivo`),
  UNIQUE KEY `UKjjaa1sha3pq26p8t3il7aidbm` (`token`),
  KEY `idx_dispositivo_token` (`token`),
  KEY `FKa6pu6pnnth9bysiliamxij3sg` (`id_usuario`),
  CONSTRAINT `FKa6pu6pnnth9bysiliamxij3sg` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lugares_fisicos`
--

DROP TABLE IF EXISTS `lugares_fisicos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lugares_fisicos` (
  `id_lugar_fisico` bigint NOT NULL AUTO_INCREMENT,
  `fecha_actualizacion` datetime(6) DEFAULT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `usuario_actualizacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usuario_creacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `activo` bit(1) NOT NULL,
  `capacidad` int DEFAULT NULL,
  `descripcion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_lugar_fisico`),
  UNIQUE KEY `UKlkoo3ia0tweesn5broa97olh6` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lugares_fisicos_aud`
--

DROP TABLE IF EXISTS `lugares_fisicos_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lugares_fisicos_aud` (
  `id_lugar_fisico` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `activo` bit(1) DEFAULT NULL,
  `capacidad` int DEFAULT NULL,
  `descripcion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_lugar_fisico`),
  CONSTRAINT `FKgxcagfy1nfm7mr3lvgg0l2u9x` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notificaciones_enviadas`
--

DROP TABLE IF EXISTS `notificaciones_enviadas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notificaciones_enviadas` (
  `exito` bit(1) NOT NULL,
  `fecha_envio` datetime(6) NOT NULL,
  `id_notificacion_enviada` bigint NOT NULL AUTO_INCREMENT,
  `tipo` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `asunto` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `destinatarios` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detalle_error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_notificacion_enviada`)
) ENGINE=InnoDB AUTO_INCREMENT=96 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oficinas`
--

DROP TABLE IF EXISTS `oficinas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oficinas` (
  `activa` bit(1) NOT NULL,
  `fecha_actualizacion` datetime(6) DEFAULT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `id_oficina` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `programa_academico` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usuario_actualizacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usuario_creacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_oficina`),
  UNIQUE KEY `UKo5t3esnb62hi9jnvtsg8mq0xm` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oficinas_aud`
--

DROP TABLE IF EXISTS `oficinas_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oficinas_aud` (
  `activa` bit(1) DEFAULT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `id_oficina` bigint NOT NULL,
  `nombre` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `programa_academico` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_creacion` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_oficina`),
  CONSTRAINT `FK1l7li5meqv4r0x040et6rdv1s` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publicaciones_anuncio`
--

DROP TABLE IF EXISTS `publicaciones_anuncio`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `publicaciones_anuncio` (
  `visible` bit(1) NOT NULL,
  `fecha_publicacion` datetime(6) NOT NULL,
  `id_publicacion_anuncio` bigint NOT NULL AUTO_INCREMENT,
  `id_solicitud_anuncio` bigint NOT NULL,
  `id_usuario_publicador` bigint NOT NULL,
  `titulo_visible` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `pieza_grafica_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion_visible` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_publicacion_anuncio`),
  KEY `FKblt2jsiqoa4ddgnkr6q5xiae` (`id_solicitud_anuncio`),
  KEY `FKyvg38mxdv7579ewetio06jau` (`id_usuario_publicador`),
  CONSTRAINT `FKblt2jsiqoa4ddgnkr6q5xiae` FOREIGN KEY (`id_solicitud_anuncio`) REFERENCES `solicitudes_anuncio` (`id_solicitud_anuncio`),
  CONSTRAINT `FKyvg38mxdv7579ewetio06jau` FOREIGN KEY (`id_usuario_publicador`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publicaciones_anuncio_aud`
--

DROP TABLE IF EXISTS `publicaciones_anuncio_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `publicaciones_anuncio_aud` (
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `visible` bit(1) DEFAULT NULL,
  `fecha_publicacion` datetime(6) DEFAULT NULL,
  `id_publicacion_anuncio` bigint NOT NULL,
  `id_solicitud_anuncio` bigint DEFAULT NULL,
  `id_usuario_publicador` bigint DEFAULT NULL,
  `titulo_visible` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pieza_grafica_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion_visible` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_publicacion_anuncio`),
  CONSTRAINT `FK64945j5w9qywv2mist32tsfpv` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publicaciones_evento`
--

DROP TABLE IF EXISTS `publicaciones_evento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `publicaciones_evento` (
  `visible` bit(1) NOT NULL,
  `fecha_publicacion` datetime(6) NOT NULL,
  `id_publicacion_evento` bigint NOT NULL AUTO_INCREMENT,
  `id_solicitud_evento` bigint NOT NULL,
  `id_usuario_publicador` bigint NOT NULL,
  `titulo_visible` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `pieza_grafica_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion_visible` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_publicacion_evento`),
  KEY `FKirbargk06c3ihjphqa8ataogp` (`id_solicitud_evento`),
  KEY `FKgu1snd2qnfg0hta8648hk2vbl` (`id_usuario_publicador`),
  KEY `idx_publicacion_evento_visible` (`visible`),
  KEY `idx_publicacion_evento_solicitud` (`id_solicitud_evento`),
  CONSTRAINT `FKgu1snd2qnfg0hta8648hk2vbl` FOREIGN KEY (`id_usuario_publicador`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `FKirbargk06c3ihjphqa8ataogp` FOREIGN KEY (`id_solicitud_evento`) REFERENCES `solicitudes_evento` (`id_solicitud`)
) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publicaciones_evento_aud`
--

DROP TABLE IF EXISTS `publicaciones_evento_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `publicaciones_evento_aud` (
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `visible` bit(1) DEFAULT NULL,
  `fecha_publicacion` datetime(6) DEFAULT NULL,
  `id_publicacion_evento` bigint NOT NULL,
  `id_solicitud_evento` bigint DEFAULT NULL,
  `id_usuario_publicador` bigint DEFAULT NULL,
  `titulo_visible` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pieza_grafica_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion_visible` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_publicacion_evento`),
  CONSTRAINT `FK5i6ut47urcuvd0emnhtpkqbtc` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reportes_generados`
--

DROP TABLE IF EXISTS `reportes_generados`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reportes_generados` (
  `fecha_desde` date NOT NULL,
  `fecha_hasta` date NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `id_oficina` bigint DEFAULT NULL,
  `id_reporte_generado` bigint NOT NULL AUTO_INCREMENT,
  `id_tipo_evento` bigint DEFAULT NULL,
  `id_usuario_generador` bigint NOT NULL,
  `formato` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `alcance` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_reporte_generado`),
  KEY `FK7d3y1ckuteyxgdc5mkde73up0` (`id_usuario_generador`),
  CONSTRAINT `FK7d3y1ckuteyxgdc5mkde73up0` FOREIGN KEY (`id_usuario_generador`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `revinfo`
--

DROP TABLE IF EXISTS `revinfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `revinfo` (
  `rev` int NOT NULL AUTO_INCREMENT,
  `revtstmp` bigint NOT NULL,
  `nombre_usuario` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id` int NOT NULL,
  `timestamp` bigint NOT NULL,
  PRIMARY KEY (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cabecera de todas las revisiones de auditoría (Hibernate Envers)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `revinfo_seq`
--

DROP TABLE IF EXISTS `revinfo_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `revinfo_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `fecha_creacion` datetime(6) DEFAULT NULL,
  `id_rol` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_rol`),
  UNIQUE KEY `UKldv0v52e0udsh2h1rs0r0gw1n` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles_aud`
--

DROP TABLE IF EXISTS `roles_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles_aud` (
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `fecha_creacion` datetime(6) DEFAULT NULL,
  `id_rol` bigint NOT NULL,
  `nombre` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_rol`),
  CONSTRAINT `FK2t5l9cik767idoi8claa5rnxo` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitud_anuncio_lugares`
--

DROP TABLE IF EXISTS `solicitud_anuncio_lugares`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitud_anuncio_lugares` (
  `id_solicitud_anuncio` bigint NOT NULL,
  `id_lugar_fisico` bigint NOT NULL,
  KEY `FK5yk229jt2cw1g7idlgs8k9wbd` (`id_lugar_fisico`),
  KEY `FKo5f70w31axgn7tj4b4h34uhdk` (`id_solicitud_anuncio`),
  CONSTRAINT `FK5yk229jt2cw1g7idlgs8k9wbd` FOREIGN KEY (`id_lugar_fisico`) REFERENCES `lugares_fisicos` (`id_lugar_fisico`),
  CONSTRAINT `FKo5f70w31axgn7tj4b4h34uhdk` FOREIGN KEY (`id_solicitud_anuncio`) REFERENCES `solicitudes_anuncio` (`id_solicitud_anuncio`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitud_anuncio_lugares_aud`
--

DROP TABLE IF EXISTS `solicitud_anuncio_lugares_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitud_anuncio_lugares_aud` (
  `rev` int NOT NULL,
  `id_solicitud_anuncio` bigint NOT NULL,
  `id_lugar_fisico` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_solicitud_anuncio`,`id_lugar_fisico`),
  CONSTRAINT `FKtauseqtpnqvjkwnuhl02mamvc` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitud_evento_lugares`
--

DROP TABLE IF EXISTS `solicitud_evento_lugares`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitud_evento_lugares` (
  `id_solicitud_evento` bigint NOT NULL,
  `id_lugar_fisico` bigint NOT NULL,
  KEY `FKtbo0dlpd9k2h077hm783u9bmt` (`id_lugar_fisico`),
  KEY `FKpes05ctpq008wddpmyjq31fst` (`id_solicitud_evento`),
  CONSTRAINT `FKpes05ctpq008wddpmyjq31fst` FOREIGN KEY (`id_solicitud_evento`) REFERENCES `solicitudes_evento` (`id_solicitud`),
  CONSTRAINT `FKtbo0dlpd9k2h077hm783u9bmt` FOREIGN KEY (`id_lugar_fisico`) REFERENCES `lugares_fisicos` (`id_lugar_fisico`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitud_evento_lugares_aud`
--

DROP TABLE IF EXISTS `solicitud_evento_lugares_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitud_evento_lugares_aud` (
  `rev` int NOT NULL,
  `id_solicitud_evento` bigint NOT NULL,
  `id_lugar_fisico` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_solicitud_evento`,`id_lugar_fisico`),
  CONSTRAINT `FKi5vwnggcqhkqw2adgm7go2jma` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitud_evento_participantes`
--

DROP TABLE IF EXISTS `solicitud_evento_participantes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitud_evento_participantes` (
  `id_participante` bigint NOT NULL AUTO_INCREMENT,
  `id_solicitud_evento` bigint NOT NULL,
  `telefono` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cargo` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `foto_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo` enum('INVITADO','ORGANIZADOR','PATROCINADOR_ALIADO') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_participante`),
  KEY `FK75dp4f3qebh1ccrumr316dw0l` (`id_solicitud_evento`),
  CONSTRAINT `FK75dp4f3qebh1ccrumr316dw0l` FOREIGN KEY (`id_solicitud_evento`) REFERENCES `solicitudes_evento` (`id_solicitud`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitud_evento_participantes_aud`
--

DROP TABLE IF EXISTS `solicitud_evento_participantes_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitud_evento_participantes_aud` (
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `id_participante` bigint NOT NULL,
  `id_solicitud_evento` bigint DEFAULT NULL,
  `telefono` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cargo` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `foto_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo` enum('INVITADO','ORGANIZADOR','PATROCINADOR_ALIADO') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_participante`),
  CONSTRAINT `FKif3ql2u6j9d89vvpnvcsipbcc` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitudes_anuncio`
--

DROP TABLE IF EXISTS `solicitudes_anuncio`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitudes_anuncio` (
  `fecha_fin_publicacion` date DEFAULT NULL,
  `fecha_inicio_publicacion` date DEFAULT NULL,
  `hora_fin` time(6) DEFAULT NULL,
  `hora_inicio` time(6) DEFAULT NULL,
  `fecha_actualizacion` datetime(6) DEFAULT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `fecha_revision` datetime(6) DEFAULT NULL,
  `id_solicitud_anuncio` bigint NOT NULL AUTO_INCREMENT,
  `id_usuario_revisor` bigint DEFAULT NULL,
  `id_usuario_solicitante` bigint NOT NULL,
  `categoria` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo_contacto` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `responsable_anuncio` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titulo` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `lugar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pieza_grafica_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `motivo_rechazo` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('APROBADA','PENDIENTE','PUBLICADA','RECHAZADA') COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_oficina` bigint DEFAULT NULL,
  `usuario_actualizacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usuario_creacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requiere_pieza_grafica` bit(1) NOT NULL,
  PRIMARY KEY (`id_solicitud_anuncio`),
  KEY `FK6gyhy3mevn3gpfb9fko8givbe` (`id_usuario_revisor`),
  KEY `FK1hxx91bwk6ccsqr8ua5dfblw` (`id_usuario_solicitante`),
  KEY `FKsus42mdqu1uq1v2opiy82ggrq` (`id_oficina`),
  CONSTRAINT `FK1hxx91bwk6ccsqr8ua5dfblw` FOREIGN KEY (`id_usuario_solicitante`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `FK6gyhy3mevn3gpfb9fko8givbe` FOREIGN KEY (`id_usuario_revisor`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `FKsus42mdqu1uq1v2opiy82ggrq` FOREIGN KEY (`id_oficina`) REFERENCES `oficinas` (`id_oficina`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitudes_anuncio_aud`
--

DROP TABLE IF EXISTS `solicitudes_anuncio_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitudes_anuncio_aud` (
  `fecha_fin_publicacion` date DEFAULT NULL,
  `fecha_inicio_publicacion` date DEFAULT NULL,
  `hora_fin` time(6) DEFAULT NULL,
  `hora_inicio` time(6) DEFAULT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `fecha_revision` datetime(6) DEFAULT NULL,
  `id_solicitud_anuncio` bigint NOT NULL,
  `id_usuario_revisor` bigint DEFAULT NULL,
  `id_usuario_solicitante` bigint DEFAULT NULL,
  `categoria` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo_contacto` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `responsable_anuncio` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titulo` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lugar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pieza_grafica_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `motivo_rechazo` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('APROBADA','PENDIENTE','PUBLICADA','RECHAZADA') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_creacion` datetime(6) DEFAULT NULL,
  `id_oficina` bigint DEFAULT NULL,
  `requiere_pieza_grafica` bit(1) DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_solicitud_anuncio`),
  CONSTRAINT `FKjcumj2d4nfxlorehmv22bd71a` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitudes_evento`
--

DROP TABLE IF EXISTS `solicitudes_evento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitudes_evento` (
  `fecha_evento` date NOT NULL,
  `hora_fin` time(6) NOT NULL,
  `hora_inicio` time(6) NOT NULL,
  `fecha_actualizacion` datetime(6) DEFAULT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `fecha_revision` datetime(6) DEFAULT NULL,
  `id_oficina` bigint NOT NULL,
  `id_solicitud` bigint NOT NULL AUTO_INCREMENT,
  `id_tipo_evento` bigint DEFAULT NULL,
  `id_usuario_revisor` bigint DEFAULT NULL,
  `id_usuario_solicitante` bigint NOT NULL,
  `responsable_evento` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre_evento` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `lugar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `link_conexion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `motivo_rechazo` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion_evento` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('APROBADA','PENDIENTE','PUBLICADA','RECHAZADA') COLLATE utf8mb4_unicode_ci NOT NULL,
  `pieza_grafica_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `es_importante` bit(1) NOT NULL,
  `observaciones` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requiere_cubrimiento` bit(1) NOT NULL,
  `requiere_transmision` bit(1) NOT NULL,
  `usuario_actualizacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usuario_creacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_fin_recurrencia` date DEFAULT NULL,
  `frecuencia_recurrencia` enum('DIARIA','MENSUAL','NINGUNA','SEMANAL') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requiere_pieza_grafica` bit(1) NOT NULL,
  `id_grupo_recurrencia` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `es_principal` bit(1) NOT NULL,
  PRIMARY KEY (`id_solicitud`),
  KEY `FK9x1a3mna12giksgf9dgisnqay` (`id_oficina`),
  KEY `FKfhfcmy0uf827ht0x5byukkxl3` (`id_tipo_evento`),
  KEY `FKfngffc6xvrc60yuobmiltgr50` (`id_usuario_revisor`),
  KEY `FK8lgtc1kigsa7aksphilg0u1we` (`id_usuario_solicitante`),
  KEY `idx_solicitud_evento_estado` (`estado`),
  KEY `idx_solicitud_evento_oficina` (`id_oficina`),
  KEY `idx_solicitud_evento_fecha` (`fecha_evento`),
  KEY `idx_solicitud_evento_grupo` (`id_grupo_recurrencia`),
  CONSTRAINT `FK8lgtc1kigsa7aksphilg0u1we` FOREIGN KEY (`id_usuario_solicitante`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `FK9x1a3mna12giksgf9dgisnqay` FOREIGN KEY (`id_oficina`) REFERENCES `oficinas` (`id_oficina`),
  CONSTRAINT `FKfhfcmy0uf827ht0x5byukkxl3` FOREIGN KEY (`id_tipo_evento`) REFERENCES `tipos_evento` (`id_tipo_evento`),
  CONSTRAINT `FKfngffc6xvrc60yuobmiltgr50` FOREIGN KEY (`id_usuario_revisor`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=122 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `solicitudes_evento_aud`
--

DROP TABLE IF EXISTS `solicitudes_evento_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitudes_evento_aud` (
  `fecha_evento` date DEFAULT NULL,
  `hora_fin` time(6) DEFAULT NULL,
  `hora_inicio` time(6) DEFAULT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `fecha_revision` datetime(6) DEFAULT NULL,
  `id_oficina` bigint DEFAULT NULL,
  `id_solicitud` bigint NOT NULL,
  `id_tipo_evento` bigint DEFAULT NULL,
  `id_usuario_revisor` bigint DEFAULT NULL,
  `id_usuario_solicitante` bigint DEFAULT NULL,
  `responsable_evento` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre_evento` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lugar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `link_conexion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `motivo_rechazo` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion_evento` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('APROBADA','PENDIENTE','PUBLICADA','RECHAZADA') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_creacion` datetime(6) DEFAULT NULL,
  `pieza_grafica_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `es_importante` bit(1) DEFAULT NULL,
  `observaciones` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requiere_cubrimiento` bit(1) DEFAULT NULL,
  `requiere_transmision` bit(1) DEFAULT NULL,
  `fecha_fin_recurrencia` date DEFAULT NULL,
  `frecuencia_recurrencia` enum('DIARIA','MENSUAL','NINGUNA','SEMANAL') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requiere_pieza_grafica` bit(1) DEFAULT NULL,
  `id_grupo_recurrencia` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `es_principal` bit(1) DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_solicitud`),
  CONSTRAINT `FK9hk1gtg0gvp5d1xq9yc82g1d` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipos_evento`
--

DROP TABLE IF EXISTS `tipos_evento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tipos_evento` (
  `activo` bit(1) NOT NULL,
  `color_hex` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_tipo_evento` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_tipo_evento`),
  UNIQUE KEY `UKpsmaewjj1lqpv4jxovyf0vqp7` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipos_evento_aud`
--

DROP TABLE IF EXISTS `tipos_evento_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tipos_evento_aud` (
  `activo` bit(1) DEFAULT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `color_hex` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_tipo_evento` bigint NOT NULL,
  `nombre` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_tipo_evento`),
  CONSTRAINT `FKd8l1ripvo637690bx9so8plwq` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `fecha_actualizacion` datetime(6) DEFAULT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `id_oficina` bigint DEFAULT NULL,
  `id_rol` bigint DEFAULT NULL,
  `id_usuario` bigint NOT NULL AUTO_INCREMENT,
  `telefono` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `foto_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `microsoft_oid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `auth_provider` enum('LOCAL','MICROSOFT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `rol` enum('ADMIN','COMUNICACIONES','OFICINA','SUPER_ADMIN','USUARIO','USUARIO_APP','USUARIO_AUTENTICADO_APP') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `usuario_actualizacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usuario_creacion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `UKcdmw5hxlfj78uf4997i3qyyw5` (`correo`),
  UNIQUE KEY `UKgh844tr3sumxkn6fo1wos65cy` (`microsoft_oid`),
  KEY `FKpfdv0pqs0sww59vfci6e2kean` (`id_oficina`),
  KEY `FK3kl77pehgupicftwfreqnjkll` (`id_rol`),
  KEY `idx_usuario_correo` (`correo`),
  KEY `idx_usuario_microsoft_oid` (`microsoft_oid`),
  CONSTRAINT `FK3kl77pehgupicftwfreqnjkll` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`),
  CONSTRAINT `FKpfdv0pqs0sww59vfci6e2kean` FOREIGN KEY (`id_oficina`) REFERENCES `oficinas` (`id_oficina`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usuarios_aud`
--

DROP TABLE IF EXISTS `usuarios_aud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios_aud` (
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `id_oficina` bigint DEFAULT NULL,
  `id_rol` bigint DEFAULT NULL,
  `id_usuario` bigint NOT NULL,
  `telefono` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `foto_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `microsoft_oid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `auth_provider` enum('LOCAL','MICROSOFT') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_creacion` datetime(6) DEFAULT NULL,
  `rol` enum('ADMIN','COMUNICACIONES','OFICINA','SUPER_ADMIN','USUARIO','USUARIO_APP','USUARIO_AUTENTICADO_APP') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`rev`,`id_usuario`),
  CONSTRAINT `FKkv8olm1917q23kc5i1ikedn3c` FOREIGN KEY (`rev`) REFERENCES `audit_revision_info` (`rev`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- View structure for view `v_anuncios_publicados`
--

DROP TABLE IF EXISTS `v_anuncios_publicados`;
/*!50001 DROP VIEW IF EXISTS `v_anuncios_publicados`*/;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50001 VIEW `v_anuncios_publicados` AS select 1 AS `id_publicacion_anuncio`,1 AS `titulo_visible`,1 AS `descripcion_visible`,1 AS `pieza_grafica_url`,1 AS `fecha_publicacion`,1 AS `categoria`,1 AS `lugar`,1 AS `fecha_inicio_publicacion`,1 AS `fecha_fin_publicacion`,1 AS `hora_inicio`,1 AS `hora_fin`,1 AS `correo_contacto`,1 AS `responsable_anuncio`,1 AS `publicado_por` */;

--
-- View structure for view `v_auditoria_reciente`
--

DROP TABLE IF EXISTS `v_auditoria_reciente`;
/*!50001 DROP VIEW IF EXISTS `v_auditoria_reciente`*/;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50001 VIEW `v_auditoria_reciente` AS select 1 AS `rev`,1 AS `revtstmp`,1 AS `fecha_cambio`,1 AS `nombre_usuario`,1 AS `ip_address`,1 AS `tabla`,1 AS `revtype`,1 AS `id_registro`,1 AS `descripcion` */;

--
-- View structure for view `v_eventos_publicados`
--

DROP TABLE IF EXISTS `v_eventos_publicados`;
/*!50001 DROP VIEW IF EXISTS `v_eventos_publicados`*/;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50001 VIEW `v_eventos_publicados` AS select 1 AS `id_publicacion_evento`,1 AS `titulo_visible`,1 AS `descripcion_visible`,1 AS `pieza_grafica_url`,1 AS `fecha_publicacion`,1 AS `fecha_evento`,1 AS `hora_inicio`,1 AS `hora_fin`,1 AS `lugar`,1 AS `link_conexion`,1 AS `responsable_evento`,1 AS `tipo_evento`,1 AS `color_hex`,1 AS `oficina`,1 AS `publicado_por` */;

--
-- View structure for view `v_solicitudes_pendientes`
--

DROP TABLE IF EXISTS `v_solicitudes_pendientes`;
/*!50001 DROP VIEW IF EXISTS `v_solicitudes_pendientes`*/;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50001 VIEW `v_solicitudes_pendientes` AS select 1 AS `tipo`,1 AS `id`,1 AS `titulo`,1 AS `estado`,1 AS `fecha_creacion`,1 AS `solicitante`,1 AS `oficina` */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-02
