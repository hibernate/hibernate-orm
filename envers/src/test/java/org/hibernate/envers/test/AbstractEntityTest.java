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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractEntityTest {
    private EntityManagerFactory emf;
    private EntityManager entityManager;
    private AuditReader versionsReader;
    private Ejb3Configuration cfg;

    public abstract void configure(Ejb3Configuration cfg);

    @BeforeMethod
    public void newEntityManager() {
        if (entityManager != null) {
            entityManager.close();
        }
        
        entityManager = emf.createEntityManager();
        versionsReader = AuditReaderFactory.get(entityManager);
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

    public AuditReader getVersionsReader() {
        return versionsReader;
    }

    public Ejb3Configuration getCfg() {
        return cfg;
    }
}
