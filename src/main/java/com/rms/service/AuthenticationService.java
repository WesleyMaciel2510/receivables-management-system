package com.rms.service;

import com.rms.dto.LoginRequest;
import com.rms.dto.LoginResponse;
import com.rms.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        JwtService.TokenData token = jwtService.generate(authentication.getName(), authentication.getAuthorities().stream().toList());
        return new LoginResponse("Bearer", token.token(), token.issuer(), token.subject(), token.issuedAt(),
                token.expiresAt(), token.authorities());
    }
}
