package com.calendario.callapp.callapp_backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
/**
 * Lista negra de JTIs revocados para soporte de logout.
 *
 * <p>Almacena en memoria ({@link ConcurrentHashMap}) los identificadores únicos ({@code jti})
 * de tokens invalidados junto con su fecha de expiración original.
 * Un {@code @Scheduled} cada 30 minutos purga los JTIs cuyos tokens ya habrían expirado
 * de todas formas, evitando que el mapa crezca sin límite.
 * Nota: al ser in-memory, la lista se pierde en cada reinicio del servidor.</p>
 */
public class TokenBlacklistService {

    // JTI → fecha de expiración original del token
    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String jti, Date expiration) {
        blacklist.put(jti, expiration);
        log.debug("Token revocado (JTI: {})", jti);
    }

    public boolean isBlacklisted(String jti) {
        return blacklist.containsKey(jti);
    }

    // Limpiar entradas cuyo token ya expiró de todas formas (cada 30 min)
    @Scheduled(fixedDelay = 1800000)
    public void cleanupExpiredEntries() {
        Date now = new Date();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(e -> e.getValue().before(now));
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.debug("TokenBlacklist cleanup: {} entradas expiradas eliminadas", removed);
        }
    }
}
