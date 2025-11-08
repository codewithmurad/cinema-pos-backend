package com.telecamnig.cinemapos.interceptor;

import java.util.Enumeration;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Lightweight request/response logger.
 * Logs method, URI, remote IP, response status and duration.
 * Does not rely on SecurityContext; will not throw if user is absent.
 */
@Slf4j
@Component
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();
    
    private static final String AUTHORIZATION = "authorization";
    
    private static final String INTERCEPTOR_MESSAGE_SEPARATOR = "=";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // record start time
        startTime.set(System.currentTimeMillis());

        try {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String ip = request.getRemoteAddr();

            // Optional: collect non-sensitive headers for debug (skip Authorization)
            StringBuilder headers = new StringBuilder();
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    if (!AUTHORIZATION.equalsIgnoreCase(name)) {
                        headers.append(name).append(INTERCEPTOR_MESSAGE_SEPARATOR).append(request.getHeader(name)).append(", ");
                    }
                }
            }

            log.info("[{}] {} | IP={} | Headers={}", method, uri, ip, headers.toString());
        } catch (Exception e) {
            // defensive: never allow logging to break request processing
            log.debug("Request logging preHandle error (ignored): {}", e.getMessage());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {

        long start = 0L;
        try {
            Long st = startTime.get();
            start = (st != null) ? st : System.currentTimeMillis();
        } finally {
            startTime.remove(); // always cleanup
        }

        long duration = System.currentTimeMillis() - start;

        try {
            int status = response.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String ip = request.getRemoteAddr();

            if (status >= 200 && status < 300) {
                log.info("[{}] {} | IP={} | Status={} | Time={}ms", method, uri, ip, status, duration);
            } else if (status >= 400 && status < 500) {
                log.warn("[{}] {} | IP={} | Status={} | Time={}ms", method, uri, ip, status, duration);
            } else if (status >= 500) {
                log.error("[{}] {} | IP={} | Status={} | Time={}ms", method, uri, ip, status, duration);
            } else {
                log.info("[{}] {} | IP={} | Status={} | Time={}ms", method, uri, ip, status, duration);
            }

            if (ex != null) {
                log.error("Exception during request {} {}: {}", method, uri, ex.getMessage(), ex);
            }
        } catch (Exception e) {
            // defensive: ensure logging does not throw
            log.debug("Request logging afterCompletion error (ignored): {}", e.getMessage());
        }
    }
}
