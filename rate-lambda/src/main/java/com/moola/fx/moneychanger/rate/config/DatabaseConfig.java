package com.moola.fx.moneychanger.rate.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class DatabaseConfig {

    private static final EntityManagerFactory EMF;

    static {
        try {
            EMF = Persistence.createEntityManagerFactory("ratePU");
        } catch (Exception e) {
            throw new DatabaseInitializationException("Error creating EMF", e);
        }
    }
    private DatabaseConfig() {}

    public static EntityManagerFactory getEntityManagerFactory() {
        return EMF;
    }

    // Custom Exception
    public static class DatabaseInitializationException extends RuntimeException {
        public DatabaseInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
