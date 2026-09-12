package com.ecommerce.commonsecurity.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import com.ecommerce.commonsecurity.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtService {

	private final JwtProperties jwtProperties;

	private SecretKey getSigningKey() {

	    return Keys.hmacShaKeyFor(
	            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
	    );
	}

	public String generateToken(String username, Long userId, String role) {

		return generateToken(Map.of(), username, userId, role);
	}

	public String generateToken(Map<String, Object> extraClaims, String username, Long userId, String role) {

		return Jwts.builder().claims(extraClaims).subject(username).claim("userId", userId).claim("role", role)
				.issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
				.signWith(getSigningKey()).compact();
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Long extractUserId(String token) {

		return extractAllClaims(token).get("userId", Long.class);
	}

	public String extractRole(String token) {

		return extractAllClaims(token).get("role", String.class);
	}

	public boolean isTokenValid(String token, String username) {

		return username.equals(extractUsername(token)) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {

		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {

		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> resolver) {

		return resolver.apply(extractAllClaims(token));
	}

	private Claims extractAllClaims(String token) {

		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
	}
	

}
