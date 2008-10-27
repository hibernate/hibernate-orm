package org.jboss.envers.test;

import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.jboss.envers.VersionsReader;
import org.jboss.envers.VersionsReaderFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractEntityTest {
    private EntityManagerFactory emf;
    private EntityManager entityManager;
    private VersionsReader versionsReader;
    private Ejb3Configuration cfg;

    public abstract void configure(Ejb3Configuration cfg);

    @BeforeMethod
    public void newEntityManager() {
        if (entityManager != null) {
            entityManager.close();
        }
        
        entityManager = emf.createEntityManager();
        versionsReader = VersionsReaderFactory.get(entityManager);
    }

    @BeforeClass
    public void init() throws IOException {
        cfg = new Ejb3Configuration();
        cfg.configure("hibernate.test.cfg.xml");
        configure(cfg);
        emf = cfg.buildEntityManagerFactory();

        newEntityManager();
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public VersionsReader getVersionsReader() {
        return versionsReader;
    }

    public Ejb3Configuration getCfg() {
        return cfg;
    }
}
