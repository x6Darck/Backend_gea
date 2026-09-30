/**
 * Interfaces MapStruct para conversión entre entidades JPA y DTOs de respuesta.
 *
 * <p>MapStruct genera las implementaciones en tiempo de compilación mediante
 * {@code build_runner} (equivalente: {@code mvn compile}). Todos los mappers
 * usan {@code componentModel = "spring"} para ser inyectados como beans Spring.
 * Mappers disponibles:</p>
 * <ul>
 *   <li>{@code LugarFisicoMapper} — {@code LugarFisico} ↔ {@code LugarFisicoResponse}.</li>
 *   <li>{@code SolicitudEventoMapper} — {@code SolicitudEvento} → {@code SolicitudEventoResponse}.</li>
 *   <li>{@code PublicacionEventoMapper} — {@code PublicacionEvento} → {@code PublicacionEventoResponse}.</li>
 *   <li>{@code SolicitudAnuncioMapper} — {@code SolicitudAnuncio} → {@code SolicitudAnuncioResponse}.</li>
 *   <li>{@code PublicacionAnuncioMapper} — {@code PublicacionAnuncio} → {@code PublicacionAnuncioResponse}.</li>
 * </ul>
 */
package com.calendario.callapp.callapp_backend.mapper;
