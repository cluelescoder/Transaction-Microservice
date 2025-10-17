package com.lloyds.transaction.security;

import com.lloyds.transaction.service.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    @Mock
    RedisService redisService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtRequestFilter jwtRequestFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ValidJwt() throws ServletException, IOException {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractId(token)).thenReturn("12345");

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil, times(1)).isTokenValid(token);
        verify(jwtUtil, times(1)).extractId(token);
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(UsernamePasswordAuthenticationToken.class);
    }

    @Test
    void doFilterInternal_BlacklistedToken() throws Exception {
        String token = "blacklisted.jwt.token";
        String jti = "blacklisted-jti";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractJti(token)).thenReturn(jti);
        when(redisService.isTokenBlacklisted(jti)).thenReturn(true);

        // Mock HttpServletResponse with a real PrintWriter
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        mockResponse.setCharacterEncoding("UTF-8");
        mockResponse.setContentType("application/json");
        mockResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);

        when(response.getWriter()).thenReturn(printWriter);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Flush and check output
        printWriter.flush();
        assertThat(stringWriter.toString()).contains("{\"error\": \"Token is blacklisted\"}");

        // Verify response status and content type
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");

        // Verify that filterChain.doFilter() was never called
        verify(filterChain, never()).doFilter(request, response);
    }


    @Test
    void doFilterInternal_InvalidJwt() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(false);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil, times(1)).isTokenValid(token);
        verify(jwtUtil, never()).extractId(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_NoAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil, never()).isTokenValid(anyString());
        verify(jwtUtil, never()).extractId(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_EmptyAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("");

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil, never()).isTokenValid(anyString());
        verify(jwtUtil, never()).extractId(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_JwtPresentButAlreadyAuthenticated() throws ServletException, IOException {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("12345", null, null)
        );
        jwtRequestFilter.doFilterInternal(request, response, filterChain);
        verify(jwtUtil, never()).isTokenValid(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ActuatorEndpoint_ShouldNotFilter() throws ServletException {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        boolean result = jwtRequestFilter.shouldNotFilter(request);

        assertThat(result).isTrue();
    }

}
