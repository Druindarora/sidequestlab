package dev.sidequestlab.backend.config;

import jakarta.servlet.Filter;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityConfigTest {

    @Test
    void csrfCookieFilterContinuesWhenRequestHasNoCsrfToken() throws Exception {
        Filter filter = newCsrfCookieFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void csrfCookieFilterMaterializesTokenWhenPresent() throws Exception {
        Filter filter = newCsrfCookieFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/csrf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        CsrfToken csrfToken = mock(CsrfToken.class);
        request.setAttribute(CsrfToken.class.getName(), csrfToken);

        filter.doFilter(request, response, chain);

        verify(csrfToken).getToken();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private Filter newCsrfCookieFilter() throws Exception {
        Class<?> filterClass = Class.forName("dev.sidequestlab.backend.config.SecurityConfig$CsrfCookieFilter");
        Constructor<?> constructor = filterClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (Filter) constructor.newInstance();
    }
}
