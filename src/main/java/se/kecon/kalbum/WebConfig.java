package se.kecon.kalbum;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig for configuring resources and headers
 *
 * @author Kenny Colliander
 * @since 2024-06-24
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CacheControlInterceptor()).addPathPatterns("/index.html");
        registry.addInterceptor(new CacheControlInterceptor()).addPathPatterns("/login");
        registry.addInterceptor(new CacheControlInterceptor()).addPathPatterns("/album/");
    }

    private static class CacheControlInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (request.getMethod().equals("GET")) {
                response.setHeader("Cache-Control", "no-store");
            }
            return true;
        }
    }
}