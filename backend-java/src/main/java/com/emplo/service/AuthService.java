package com.emplo.service;

import com.emplo.config.AppProperties;
import com.emplo.dto.auth.*;
import com.emplo.entity.Employee;
import com.emplo.entity.Role;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.UnauthorizedException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.RoleRepository;
import com.emplo.repository.UserRepository;
import com.emplo.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public CurrentUserResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        // SECURITY: Public registration is locked to 'employee' role only.
        // Manager and HR accounts must be created by an existing HR admin via the employee management flow.
        if (request.getRole() != null && request.getRole() != RoleName.employee) {
            throw new BadRequestException("Public registration is only available for the 'employee' role. Contact HR for elevated access.");
        }

        Role role = roleRepository.findByName(RoleName.employee)
                .orElseThrow(() -> new BadRequestException("Role not found. Seed roles first."));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roleId(role.getId())
                .isActive(true)
                .build();
        user = userRepository.save(user);

        return CurrentUserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .role(role.getName())
                .build();
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.getIsActive()) {
            throw new com.emplo.exception.ForbiddenException("Account is inactive");
        }
        return TokenResponse.builder()
                .accessToken(jwtService.createAccessToken(user.getId(), user.getRole().getName().name()))
                .refreshToken(jwtService.createRefreshToken(user.getId()))
                .tokenType("bearer")
                .build();
    }

    public TokenResponse refresh(RefreshRequest request) {
        Claims claims = jwtService.parseToken(request.getRefreshToken());
        if (claims == null || !"refresh".equals(jwtService.extractTokenType(claims))) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        UUID userId = jwtService.extractUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found or inactive"));
        if (!user.getIsActive()) {
            throw new UnauthorizedException("User not found or inactive");
        }
        return TokenResponse.builder()
                .accessToken(jwtService.createAccessToken(user.getId(), user.getRole().getName().name()))
                .refreshToken(jwtService.createRefreshToken(user.getId()))
                .tokenType("bearer")
                .build();
    }

    public CurrentUserResponse getCurrentUser(User user) {
        String name = null;
        String profilePhoto = null;
        if (user.getEmployeeId() != null) {
            Employee emp = employeeRepository.findById(user.getEmployeeId()).orElse(null);
            if (emp != null) {
                name = emp.getFullName();
                profilePhoto = emp.getProfilePhoto();
            }
        }
        return CurrentUserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .name(name)
                .profilePhoto(profilePhoto)
                .build();
    }
}
