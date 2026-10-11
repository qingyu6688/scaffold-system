package com.scaffold.system.service;

import com.scaffold.framework.security.SessionAccountResolver;
import com.scaffold.system.mapper.AuthMapper;
import org.springframework.stereotype.Service;

@Service
public class SystemSessionAccountResolver implements SessionAccountResolver {
    private final AuthMapper mapper;

    public SystemSessionAccountResolver(AuthMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isActive(String username, long version) {
        var user = mapper.selectUserByUsername(username);
        return user != null && Integer.valueOf(1).equals(user.getStatus())
                && user.getSessionVersion() == version;
    }
}
