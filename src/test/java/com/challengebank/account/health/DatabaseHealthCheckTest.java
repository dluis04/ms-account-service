package com.challengebank.account.health;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthCheckTest {

    @Mock
    EntityManager entityManager;

    @Mock
    Query query;

    @InjectMocks
    DatabaseHealthCheck healthCheck;

    @Test
    void testHealthy() {
        when(entityManager.createNativeQuery("SELECT 1")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);

        HealthCheckResponse response = healthCheck.call();

        assertEquals("Database connection", response.getName());
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
    }

    @Test
    void testUnhealthy() {
        when(entityManager.createNativeQuery("SELECT 1")).thenThrow(new RuntimeException("Connection refused"));

        HealthCheckResponse response = healthCheck.call();

        assertEquals("Database connection", response.getName());
        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
    }
}
