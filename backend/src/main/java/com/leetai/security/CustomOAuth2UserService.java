package com.leetai.security;

import com.leetai.model.User;
import com.leetai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Runs after Google/GitHub confirms who the person is. OAuth2 login only
 * ever proves identity — it NEVER grants admin by itself. Admin status
 * comes only from ADMIN_EMAILS, an env var this app controls; nothing the
 * client sends can influence it.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Value("${app.admin-emails:}")
    private String adminEmailsRaw;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(request);
        String provider = request.getClientRegistration().getRegistrationId(); // "google" | "github"

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String avatar = oauthUser.getAttribute("picture") != null
                ? oauthUser.getAttribute("picture")
                : oauthUser.getAttribute("avatar_url");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Provider did not return an email address");
        }

        User user = userRepository.findByEmail(email).orElseGet(User::new);
        boolean isNew = user.getId() == null;

        user.setEmail(email);
        user.setName(name);
        user.setAvatarUrl(avatar);
        user.setProvider(provider);

        // Admin status is re-checked on every login against the allowlist —
        // add/remove an email from ADMIN_EMAILS and it takes effect on next
        // login. Existing admins not in the list (e.g. promoted manually in
        // the DB) are left alone rather than silently demoted.
        if (isAdminEmail(email)) {
            user.setRole(User.Role.ADMIN);
        } else if (isNew) {
            user.setRole(User.Role.USER);
        }

        userRepository.save(user);
        return oauthUser;
    }

    private boolean isAdminEmail(String email) {
        Set<String> allowlist = parseAllowlist();
        return allowlist.contains(email.toLowerCase());
    }

    private Set<String> parseAllowlist() {
        if (adminEmailsRaw == null || adminEmailsRaw.isBlank()) return Set.of();
        List<String> emails = Arrays.stream(adminEmailsRaw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();
        return Set.copyOf(emails);
    }
}
