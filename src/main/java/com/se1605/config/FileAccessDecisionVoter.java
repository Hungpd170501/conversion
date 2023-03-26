package com.se1605.config;

import com.se1605.entity.FileEntity;
import com.se1605.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;

import java.util.Collection;

public class FileAccessDecisionVoter implements AccessDecisionVoter<Object> {
    private FileService fileService;

    public FileAccessDecisionVoter(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ACCESS_DENIED;
        }

        if (!(object instanceof FilterInvocation)) {
            return ACCESS_ABSTAIN;
        }

        FilterInvocation filterInvocation = (FilterInvocation) object;
        HttpServletRequest request = filterInvocation.getHttpRequest();
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (!path.startsWith("/DocumentSamples/")) {
            return ACCESS_ABSTAIN;
        }

        Long fileId = Long.parseLong(path.substring("/DocumentSamples/".length()));
        FileEntity file = fileService.findById(fileId).orElse(null);
        if (file == null) {
            return ACCESS_DENIED;
        }

        if (!file.getUser().getEmail().equals(authentication.getName())) {
            return ACCESS_DENIED;
        }

        return ACCESS_GRANTED;
    }
}
