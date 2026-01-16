package com.example.securetx.security.dto;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.securetx.auth.entity.User;

public class SecurityUser implements UserDetails {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7930208913381001019L;
	private final User user;

	public SecurityUser(User user) {
		this.user = user;
	}

	public User getDomainUser() {
		return user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// user.role should be like "ROLE_USER" / "ROLE_ADMIN"
		return List.of(new SimpleGrantedAuthority(user.getRole()));
	}

	@Override
	public String getPassword() {
		return user.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return Boolean.TRUE.equals(user.getActive());
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return Boolean.TRUE.equals(user.getActive());
	}
}
