/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
import java.util.Properties;
import javax.persistence.EntityManager;

import org.junit.Before;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.event.EnversIntegrator;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.BootstrapServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractEntityTest extends AbstractEnversTest {
    private EntityManagerFactoryImpl emf;
    private EntityManager entityManager;
    private AuditReader auditReader;
    private Ejb3Configuration cfg;
	private StandardServiceRegistryImpl serviceRegistry;
    private boolean audited;

    public abstract void configure(Ejb3Configuration cfg);

    public void addConfigurationProperties(Properties configuration) { }

    private void closeEntityManager() {
        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }
    }

    @Before
    public void newEntityManager() {
        closeEntityManager();
        
        entityManager = emf.createEntityManager();

        if (audited) {
            auditReader = AuditReaderFactory.get(entityManager);
        }
    }

    @BeforeClassOnce
    public void init() throws IOException {
        init(true, getAuditStrategy());
    }

    protected void init(boolean audited, String auditStrategy) throws IOException {
        this.audited = audited;

        Properties configurationProperties = new Properties();
		configurationProperties.putAll( Environment.getProperties() );
        if (!audited) {
			configurationProperties.setProperty(EnversIntegrator.AUTO_REGISTER, "false");
        }
		if ( createSchema() ) {
			configurationProperties.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		}
        if (auditStrategy != null && !"".equals(auditStrategy)) {
            configurationProperties.setProperty("org.hibernate.envers.audit_strategy", auditStrategy);
        }

        addConfigurationProperties(configurationProperties);

        cfg = new Ejb3Configuration();
        configure(cfg);
        cfg.configure(configurationProperties);

        emf = (EntityManagerFactoryImpl) cfg.buildEntityManagerFactory( createBootstrapRegistryBuilder() );

		serviceRegistry = (StandardServiceRegistryImpl) ( (SessionFactoryImpl) emf.getSessionFactory() ).getServiceRegistry().getParentServiceRegistry();

        newEntityManager();
    }
	protected boolean createSchema() {
		return true;
	}

	private BootstrapServiceRegistryBuilder createBootstrapRegistryBuilder() {
		return new BootstrapServiceRegistryBuilder();
	}


	@AfterClassOnce
    public void close() {
        closeEntityManager();
        emf.close();
		//NOTE we don't build the service registry so we don't destroy it
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
