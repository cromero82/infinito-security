package com.infinitosoft.infinitosecurity.security;
import com.infinitosoft.infinitosecurity.config.JwtProperties;
import com.infinitosoft.infinitosecurity.dto.UserInfo;
import com.infinitosoft.infinitosecurity.model.Rol;
import com.infinitosoft.infinitosecurity.model.Usuario;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.DecodingException;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.OffsetDateTime;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    private Key signingKey() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret no configurado (jwt.secret)");
        }
        byte[] keyBytes;
        // Evitar intentar decodificar valores obviamente no-Base64
        if (looksLikeBase64(secret)) {
            try {
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (DecodingException | IllegalArgumentException ex) {
                // Si la decodificación falla, usar el texto plano como bytes UTF-8
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean looksLikeBase64(String s) {
        // A-Z a-z 0-9 + / = y longitud múltiplo de 4, sin espacios
        if (s.indexOf(' ') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\t') >= 0) {
            return false;
        }
        int len = s.length();
        if (len == 0 || (len % 4) != 0) return false;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=';
            if (!ok) return false;
        }
        return true;
    }

    public String generateToken(String jti, Usuario usuario) {
        Date now = new Date();
        Date exp = Date.from(OffsetDateTime.now().plusDays(properties.getExpirationDays()).toInstant());
        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", jti);
        claims.put("nombre", usuario.getNombre());
        claims.put("telefono", usuario.getTelefono());
        // Incluir roles como objetos con sigla y nombre para que el consumidor pueda mostrar ambos.
        // Nota: tokens antiguos guardaban solo la sigla (List<String>). getUserInfo mantiene compatibilidad.
        List<Map<String, Object>> rolesClaim = usuario.getRoles().stream()
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("sigla", r.getSigla());
                    m.put("nombre", r.getNombre());
                    return m;
                })
                .collect(Collectors.toList());
        claims.put("roles", rolesClaim);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(usuario.getCorreoElectronico())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseAllClaims(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public String getJti(String token) {
        Claims claims = parseAllClaims(token);
        Object jti = claims.get("jti");
        return jti != null ? jti.toString() : null;
    }

    public UserInfo getUserInfo(String token) {
        Claims claims = parseAllClaims(token);
        String nombre = claims.get("nombre", String.class);
        String correo = claims.getSubject();
        String telefono = claims.get("telefono", String.class);
        Object rolesObj = claims.get("roles");
        List<Rol> rolList = List.of();
        if (rolesObj instanceof List<?>) {
            List<?> list = (List<?>) rolesObj;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                // Formato nuevo: List<Map> con {sigla, nombre}
                rolList = list.stream()
                        .map(it -> (Map<?, ?>) it)
                        .map(m -> Rol.builder()
                                .sigla(Objects.toString(m.get("sigla"), null))
                                .nombre(Objects.toString(m.get("nombre"), null))
                                .build())
                        .collect(Collectors.toList());
            } else if (!list.isEmpty() && list.get(0) instanceof String) {
                // Formato legado: List<String> (solo siglas)
                rolList = list.stream()
                        .map(String.class::cast)
                        .map(sigla -> Rol.builder().sigla(sigla).build())
                        .collect(Collectors.toList());
            } else if (list.isEmpty()) {
                rolList = List.of();
            }
        }
        return UserInfo.builder()
                .nombre(nombre)
                .correoElectronico(correo)
                .telefono(telefono)
                .roles(rolList)
                .build();
    }

    public Date getExpirationDate(String token) {
        Claims claims = parseAllClaims(token);
        return claims.getExpiration();
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(token).getBody();
    }
}
