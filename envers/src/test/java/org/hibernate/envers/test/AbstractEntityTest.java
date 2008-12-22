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

import java.io.IOException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.event.AuditEventListener;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterClass;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.event.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractEntityTest {
    private EntityManagerFactory emf;
    private EntityManager entityManager;
    private AuditReader auditReader;
    private Ejb3Configuration cfg;
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
    public void init() throws IOException {
        init(true);
    }

    protected void init(boolean audited) throws IOException {
        this.audited = audited;

        cfg = new Ejb3Configuration();
        if (audited) {
            initListeners();
        }
        cfg.configure("hibernate.test.cfg.xml");
        configure(cfg);
        emf = cfg.buildEntityManagerFactory();

        newEntityManager();
    }

    @AfterClass
    public void close() {
        closeEntityManager();
        emf.close();
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
}
