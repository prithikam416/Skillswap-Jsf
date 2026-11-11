package com.skillswap.util;

import com.skillswap.entity.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.*;
import java.util.function.Function;

/**
 * JPA / Hibernate bootstrap.
 *
 * The persistence unit is built in code (no persistence.xml needed), using the same connection
 * settings as {@link DBUtil}. The tables themselves are still created / upgraded by DBUtil, so
 * Hibernate only has to map them (hbm2ddl is off).
 */
public final class JpaUtil {
    private static final List<String> ENTITIES = Arrays.asList(
            UserEntity.class.getName(), SkillEntity.class.getName(), UserSkillEntity.class.getName(),
            ExchangeRequestEntity.class.getName(), SessionEntity.class.getName(), RatingEntity.class.getName());

    private static EntityManagerFactory emf;

    private JpaUtil() {}

    /** True when the database is reachable (JPA is then the persistence path for requests, sessions, ratings). */
    public static boolean isAvailable() {
        return DBUtil.isDatabaseAvailable();
    }

    private static synchronized EntityManagerFactory factory() {
        if (emf == null || !emf.isOpen()) {
            Map<String, Object> props = new HashMap<>();
            props.put("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
            props.put("hibernate.connection.url", DBUtil.jdbcUrl());
            props.put("hibernate.connection.username", DBUtil.jdbcUser());
            props.put("hibernate.connection.password", DBUtil.jdbcPassword());
            props.put("hibernate.connection.pool_size", "8");
            props.put("hibernate.hbm2ddl.auto", "none");
            props.put("hibernate.show_sql", "false");
            props.put("hibernate.archive.autodetection", "");
            emf = new org.hibernate.jpa.HibernatePersistenceProvider()
                    .createContainerEntityManagerFactory(new Unit(), props);
            System.out.println("[SkillSwap] JPA/Hibernate started (" + ENTITIES.size() + " entities).");
        }
        return emf;
    }

    public static EntityManager em() {
        return factory().createEntityManager();
    }

    /** Runs a read-only unit of work. */
    public static <T> T read(Function<EntityManager, T> work) {
        EntityManager em = em();
        try {
            return work.apply(em);
        } finally {
            em.close();
        }
    }

    /** Runs a unit of work inside a transaction (commit on success, rollback on any exception). */
    public static <T> T write(Function<EntityManager, T> work) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.apply(em);
            em.flush();
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static synchronized void shutdown() {
        if (emf != null && emf.isOpen()) emf.close();
        emf = null;
    }

    /** Describes the persistence unit to Hibernate (replaces META-INF/persistence.xml). */
    private static final class Unit implements PersistenceUnitInfo {
        public String getPersistenceUnitName() { return "skillswapPU"; }
        public String getPersistenceProviderClassName() { return "org.hibernate.jpa.HibernatePersistenceProvider"; }
        public PersistenceUnitTransactionType getTransactionType() { return PersistenceUnitTransactionType.RESOURCE_LOCAL; }
        public DataSource getJtaDataSource() { return null; }
        public DataSource getNonJtaDataSource() { return null; }
        public List<String> getMappingFileNames() { return Collections.emptyList(); }
        public List<URL> getJarFileUrls() { return Collections.emptyList(); }
        public URL getPersistenceUnitRootUrl() { return null; }
        public List<String> getManagedClassNames() { return ENTITIES; }
        public boolean excludeUnlistedClasses() { return true; }
        public SharedCacheMode getSharedCacheMode() { return SharedCacheMode.NONE; }
        public ValidationMode getValidationMode() { return ValidationMode.NONE; }
        public Properties getProperties() { return new Properties(); }
        public String getPersistenceXMLSchemaVersion() { return "2.2"; }
        public ClassLoader getClassLoader() { return JpaUtil.class.getClassLoader(); }
        public void addTransformer(ClassTransformer transformer) { }
        public ClassLoader getNewTempClassLoader() { return JpaUtil.class.getClassLoader(); }
    }
}
