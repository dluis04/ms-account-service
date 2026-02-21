package com.challengebank.account.logging;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    ContainerRequestContext requestContext;

    @Mock
    ContainerResponseContext responseContext;

    @InjectMocks
    CorrelationIdFilter filter;

    @AfterEach
    void cleanUp() {
        MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
    }

    @Test
    void testFilter_existingCorrelationId() {
        String existingId = "existing-correlation-id-123";
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        when(requestContext.getHeaderString(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(existingId);
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        assertEquals(existingId, MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
        assertEquals(existingId, headers.getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void testFilter_noCorrelationId() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        when(requestContext.getHeaderString(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        String mdcValue = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        assertNotNull(mdcValue);
        assertFalse(mdcValue.isBlank());
        assertDoesNotThrow(() -> java.util.UUID.fromString(mdcValue));
        assertEquals(mdcValue, headers.getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void testFilter_blankCorrelationId() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        when(requestContext.getHeaderString(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("   ");
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        String mdcValue = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        assertNotNull(mdcValue);
        assertDoesNotThrow(() -> java.util.UUID.fromString(mdcValue));
        assertEquals(mdcValue, headers.getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void testResponseFilter_addsHeader() {
        String correlationId = "response-correlation-id-456";
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);

        MultivaluedMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(responseHeaders);

        filter.filter(requestContext, responseContext);

        assertEquals(correlationId, responseHeaders.getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void testResponseFilter_cleansMDC() {
        String correlationId = "cleanup-correlation-id-789";
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);

        MultivaluedMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(responseHeaders);

        filter.filter(requestContext, responseContext);

        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    @Test
    void testResponseFilter_noCorrelationId_inMDC() {
        MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);

        filter.filter(requestContext, responseContext);

        verify(responseContext, never()).getHeaders();
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }
}
