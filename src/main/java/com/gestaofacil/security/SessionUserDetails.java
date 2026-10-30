package com.gestaofacil.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class SessionUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final boolean enabled;
    private final long credentialVersion;
    private final Collection<? extends GrantedAuthority> authorities;

    public SessionUserDetails(
            String username,
            String password,
            boolean enabled,
            long credentialVersion,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.credentialVersion = credentialVersion;
        this.authorities = authorities;
    }

    public long credentialVersion() {
        return credentialVersion;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
