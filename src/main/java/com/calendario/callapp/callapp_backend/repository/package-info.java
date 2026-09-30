/**
 * Capa de acceso a datos con Spring Data JPA (PostgreSQL).
 *
 * <p>Cada interfaz extiende {@code JpaRepository} y es usada exclusivamente desde
 * la capa de servicios ({@code service/impl}). Convenciones del paquete:</p>
 *
 * <ul>
 *   <li>Los métodos con {@code JOIN FETCH} precargan asociaciones críticas para evitar N+1.</li>
 *   <li>Los métodos de agregación para reportes devuelven {@code List<Object[]>} con columnas
 *       documentadas en cada método.</li>
 *   <li>Los métodos de búsqueda con filtros opcionales usan {@code :param IS NULL OR} para
 *       ignorar el filtro cuando el valor es nulo.</li>
 * </ul>
 */
package com.calendario.callapp.callapp_backend.repository;
