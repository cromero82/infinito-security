package com.infinitosoft.infinitosecurity.security;
import com.infinitosoft.infinitosecurity.config.JwtProperties;
import com.infinitosoft.infinitosecurity.dto.UserInfo;
import com.infinitosoft.infinitosecurity.model.Rol;
import com.infinitosoft.infinitosecurity.model.Usuario;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    private Key signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(properties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String jti, Usuario usuario) {
        Date now = new Date();
        Date exp = Date.from(OffsetDateTime.now().plusDays(properties.getExpirationDays()).toInstant());
        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", jti);
        claims.put("nombre", usuario.getNombre());
        claims.put("telefono", usuario.getTelefono());
        claims.put("roles", usuario.getRoles().stream().map(Rol::getSigla).toList());

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
        List<String> roles = claims.get("roles", List.class);

        List<Rol> rolList = roles == null ? List.of() : roles.stream()
                .map(sigla -> Rol.builder().sigla(sigla).build())
                .toList();
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
