/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.performance;

import java.util.Arrays;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.boot.internal.EnversIntegrator;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.orm.test.envers.AbstractEnversTest;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.Before;

import jakarta.persistence.EntityManager;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractEntityManagerTest extends AbstractEnversTest {
	public static final Dialect DIALECT = DialectContext.getDialect();

	private SessionFactoryImplementor emf;
	private EntityManager entityManager;
	private AuditReader auditReader;
	private boolean audited;

	public void addConfigurationProperties(Properties configuration) {
	}

	protected static Dialect getDialect() {
		return DIALECT;
	}

	private void closeEntityManager() {
		if ( entityManager != null ) {
			entityManager.close();
			entityManager = null;
		}
	}

	@Before
	public void newEntityManager() {
		closeEntityManager();

		entityManager = emf.createEntityManager();

		if ( audited ) {
			auditReader = AuditReaderFactory.get( entityManager );
		}
	}

	@BeforeClassOnce
	public void init() {
		init( true, getAuditStrategy() );
	}

	protected void init(boolean audited, String auditStrategy) {
		this.audited = audited;

		Properties configurationProperties = new Properties();
		configurationProperties.putAll( Environment.getProperties() );
		if ( !audited ) {
			configurationProperties.setProperty( EnversIntegrator.AUTO_REGISTER, "false" );
		}
		if ( createSchema() ) {
			configurationProperties.setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
			configurationProperties.setProperty( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, "false" );
		}
		if ( auditStrategy != null && !auditStrategy.isEmpty() ) {
			configurationProperties.setProperty( "org.hibernate.envers.audit_strategy", auditStrategy );
		}

		addConfigurationProperties( configurationProperties );

		configurationProperties.put( AvailableSettings.LOADED_CLASSES, Arrays.asList( getAnnotatedClasses() ) );

		EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder =
				(EntityManagerFactoryBuilderImpl)
						Bootstrap.getEntityManagerFactoryBuilder(
								new PersistenceUnitDescriptorAdapter(),
								configurationProperties
						);

		emf = entityManagerFactoryBuilder.build().unwrap( SessionFactoryImplementor.class );

		newEntityManager();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[0];
	}

	protected boolean createSchema() {
		return true;
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
}
