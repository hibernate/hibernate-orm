/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.event.AuditEventListener;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreCollectionRemoveEventListener;
import org.hibernate.event.PreCollectionUpdateEventListener;
import org.hibernate.service.internal.BasicServiceRegistryImpl;

import org.hibernate.testing.jta.TestingJtaBootstrap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractEntityTest {
    private EntityManagerFactory emf;
    private EntityManager entityManager;
    private AuditReader auditReader;
    private Ejb3Configuration cfg;
	private BasicServiceRegistryImpl serviceRegistry;
    private boolean audited;

    public abstract void configure(Ejb3Configuration cfg);

    private void initListeners() {
        AuditEventListener listener = new AuditEventListener();
        cfg.getEventListeners().setPostInsertEventListeners(new PostInsertEventListener[] { listener });
        cfg.getEventListeners().setPostUpdateEventListeners(new PostUpdateEventListener[] { listener });
        cfg.getEventListeners().setPostDeleteEventListeners(new PostDeleteEventListener[] { listener });
        cfg.getEventListeners().setPreCollectionUpdateEventListeners(new PreCollectionUpdateEventListener[] { listener });
        cfg.getEventListeners().setPreCollectionRemoveEventListeners(new PreCollectionRemoveEventListener[] { listener });
        cfg.getEventListeners().setPostCollectionRecreateEventListeners(new PostCollectionRecreateEventListener[] { listener });
    }

    private void closeEntityManager() {
        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }
    }

    @BeforeMethod
    public void newEntityManager() {
        closeEntityManager();
        
        entityManager = emf.createEntityManager();

        if (audited) {
            auditReader = AuditReaderFactory.get(entityManager);
        }
    }

    @BeforeClass
    @Parameters("auditStrategy")    
    public void init(@Optional String auditStrategy) throws IOException {
        init(true, auditStrategy);
    }

    protected void init(boolean audited, String auditStrategy) throws IOException {
        this.audited = audited;

        cfg = new Ejb3Configuration();
        if (audited) {
            initListeners();
        }

        cfg.configure( "hibernate.test.cfg.xml" );

        if (auditStrategy != null && !"".equals(auditStrategy)) {
            cfg.setProperty("org.hibernate.envers.audit_strategy", auditStrategy);
        }

        // Separate database for each test class
        cfg.setProperty( Environment.URL, "jdbc:h2:mem:" + this.getClass().getName() );

        configure( cfg );

		cfg.configure( cfg.getHibernateConfiguration().getProperties() );

		serviceRegistry = new BasicServiceRegistryImpl( cfg.getProperties() );

        emf = cfg.buildEntityManagerFactory( serviceRegistry );

        newEntityManager();
    }

    @AfterClass
    public void close() {
        closeEntityManager();
        emf.close();
		serviceRegistry.destroy();
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public AuditReader getAuditReader() {
        return auditReader;
    }

    public Ejb3Configuration getCfg() {
        return cfg;
    }

    protected void addJTAConfig(Ejb3Configuration cfg) {
		TestingJtaBootstrap.prepare( cfg.getProperties() );
		cfg.getProperties().remove( Environment.USER );
		cfg.getProperties().remove( Environment.PASS );
		cfg.setProperty( AvailableSettings.TRANSACTION_TYPE, "JTA" );
    }
}
