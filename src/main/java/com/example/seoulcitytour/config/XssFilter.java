package com.example.seoulcitytour.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    static class XssRequestWrapper extends HttpServletRequestWrapper {

        // 위험한 패턴들
        private static final Pattern[] PATTERNS = {
            Pattern.compile("<script>(.*?)</script>",                        Pattern.CASE_INSENSITIVE),
            Pattern.compile("src[\\r\\n]*=[\\r\\n]*\\'(.*?)\\'",            Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("src[\\r\\n]*=[\\r\\n]*\\\"(.*?)\\\"",         Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("</script>",                                     Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>",                                 Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("eval\\((.*?)\\)",                               Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("expression\\((.*?)\\)",                         Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("javascript:",                                   Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:",                                     Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload(.*?)=",                                  Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("onerror(.*?)=",                                 Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<iframe",                                       Pattern.CASE_INSENSITIVE),
            Pattern.compile("<img[^>]+src[\\s]*=[\\s]*[\"'][\\s]*javascript", Pattern.CASE_INSENSITIVE),
        };

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getParameter(String parameter) {
            String value = super.getParameter(parameter);
            return sanitize(value);
        }

        @Override
        public String getHeader(String name) {
            return sanitize(super.getHeader(name));
        }

        private String sanitize(String value) {
            if (value == null) return null;
            String result = value;
            for (Pattern pattern : PATTERNS) {
                result = pattern.matcher(result).replaceAll("");
            }
            // HTML 특수문자 이스케이프
            result = result
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
            return result;
        }
    }
}
