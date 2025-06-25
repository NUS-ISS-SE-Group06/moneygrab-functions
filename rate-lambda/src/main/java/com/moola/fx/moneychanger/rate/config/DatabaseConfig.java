package com.moola.fx.moneychanger.rate.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class DatabaseConfig {

    private static final EntityManagerFactory EMF;

    static {
        try {
            EMF = Persistence.createEntityManagerFactory("ratePU");
            System.out.println("[DatabaseConfig] ✅ EMF created");
        } catch (Exception e) {
            System.err.println("[DatabaseConfig] ❌ Failed to initialize EMF");
            //e.printStackTrace();
            throw new RuntimeException("Error creating EMF", e);
        }
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return EMF;
    }
}
