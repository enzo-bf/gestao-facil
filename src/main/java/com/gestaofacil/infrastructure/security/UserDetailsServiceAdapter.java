package com.gestaofacil.infrastructure.security;

import com.gestaofacil.domain.User;
import com.gestaofacil.repository.UserRepository;
import com.gestaofacil.repository.PermissionRepository;
import com.gestaofacil.security.SessionUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceAdapter implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        var authorities = user.isEmailVerified()
                ? permissionRepository.findCodesByUserId(user.getId()).stream()
                .map(SimpleGrantedAuthority::new).toList()
                : java.util.List.<SimpleGrantedAuthority>of();
        return new SessionUserDetails(
                user.getEmail(), user.getPassword(), user.isActive(), user.getCredentialVersion(), authorities
        );
    }
}
