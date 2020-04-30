package com.example.pt.lab3.configuration;

import com.example.pt.lab3.service.enrich.SessionEnrichment;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static java.util.Collections.list;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

@Configuration
@RequiredArgsConstructor
public class UserRegistrationInterceptorConfig implements WebMvcConfigurer {

    private final SessionEnrichment sessionEnrichment;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(
                new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        final HttpSession session = request.getSession();
                        HttpHeaders httpHeaders = getHttpHeadersFromHttpServletRequest(request);
                        sessionEnrichment.enrichSessionWithCurrentContract(session, httpHeaders);
                        return true;
                    }
                }
        ).addPathPatterns("/api/game/**");
    }

    private static HttpHeaders getHttpHeadersFromHttpServletRequest(HttpServletRequest request) {
        return list(request.getHeaderNames())
                .stream()
                .collect(toMap(identity(),
                        h -> list(request.getHeaders(h)),
                        (oldValue, newValue) -> concat(oldValue.stream(), newValue.stream()).collect(toList()),
                        HttpHeaders::new
                ));
    }
}
