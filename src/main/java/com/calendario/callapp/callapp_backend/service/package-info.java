/**
 * Contratos (interfaces) de la capa de servicio del backend GEA.
 *
 * <p>Cada interfaz define el contrato público de un servicio de negocio,
 * independientemente de su implementación concreta. Las implementaciones
 * viven en el subpaquete {@code service/impl}.
 * Interfaces disponibles:</p>
 *
 * <ul>
 *   <li>{@code AuthService} — autenticación local (login con contraseña).</li>
 *   <li>{@code ArchivoStorageService} — almacenamiento de archivos adjuntos.</li>
 *   <li>{@code DispositivoUsuarioService} — registro de tokens FCM para push.</li>
 *   <li>{@code LugarFisicoService} — consulta de espacios físicos para eventos.</li>
 * </ul>
 */
package com.calendario.callapp.callapp_backend.service;
