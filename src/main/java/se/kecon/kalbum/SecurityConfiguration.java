package se.kecon.kalbum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import se.kecon.kalbum.auth.AlbumAuthorizationManager;
import se.kecon.kalbum.auth.FileAuthenticationManager;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@Slf4j
public class SecurityConfiguration {
    @Bean
    AuthenticationManager fileAuthenticationManager() {
        return new FileAuthenticationManager();
    }

    @Autowired
    CsrfTokenRepository csrfTokenRepository;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorizeRequests) ->
                        authorizeRequests
                                .requestMatchers("/login").permitAll()
                                .requestMatchers("/logout").permitAll()
                                .requestMatchers("/resetpassword/", "/resetpassword/**").permitAll()
                                .requestMatchers("/assets/js/password.js", "/assets/js/resetpassword.js", "/assets/js/resetpassword-new.js").permitAll()
                                .requestMatchers("/assets/css/**").permitAll()
                                .requestMatchers("/images/**").permitAll()
                                .requestMatchers("/users/", "/users/**").permitAll()
                                .requestMatchers("/albums/", "/albums/**").permitAll()
                                .anyRequest().authenticated()
                )
                .csrf((csrf) -> csrf.csrfTokenRepository(csrfTokenRepository).requireCsrfProtectionMatcher(myCustomCsrfMatcher())) // Sensitive: csrf protection is disabled for specific routes
                .formLogin((login) -> login.loginPage("/login").permitAll())
                .logout(Customizer.withDefaults());
        return http.build();
    }

    private RequestMatcher myCustomCsrfMatcher() {
        RequestMatcher usersPost = new AntPathRequestMatcher("/users/**", "POST");
        RequestMatcher usersPut = new AntPathRequestMatcher("/users/**", "PUT");
        RequestMatcher usersPatch = new AntPathRequestMatcher("/users/**", "PATCH");
        RequestMatcher usersDelete = new AntPathRequestMatcher("/users/**", "DELETE");

        RequestMatcher albumsPost = new AntPathRequestMatcher("/albums/**", "POST");
        RequestMatcher albumsPut = new AntPathRequestMatcher("/albums/**", "PUT");
        RequestMatcher albumsPatch = new AntPathRequestMatcher("/albums/**", "PATCH");
        RequestMatcher albumsDelete = new AntPathRequestMatcher("/albums/**", "DELETE");

        return new OrRequestMatcher(usersPost, usersPut, usersPatch, usersDelete, albumsPost, albumsPut, albumsPatch, albumsDelete);
    }

    @Bean
    Advisor preAuthorize(AlbumAuthorizationManager manager) {
        return AuthorizationManagerBeforeMethodInterceptor.preAuthorize(manager);
    }

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(64000);
        return loggingFilter;
    }
}