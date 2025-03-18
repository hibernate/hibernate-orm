/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Before;

/**
 * Base class for testing envers with Session when the same session and
 * auditReader must be used for the hole test.
 *
 * @author Hern&aacute;n Chanfreau
 */
public abstract class AbstractOneSessionTest extends AbstractEnversTest {
	protected Configuration config;
	private ServiceRegistry serviceRegistry;
	private SessionFactory sessionFactory;
	private Session session;
	private AuditReader auditReader;

	@BeforeClassOnce
	public void init() throws URISyntaxException {
		config = new Configuration();
		URL url = Thread.currentThread().getContextClassLoader().getResource( getHibernateConfigurationFileName() );
		config.configure( new File( url.toURI() ) );
		// Envers tests expect sequences to not skip values...
		config.setProperty( EnversSettings.REVISION_SEQUENCE_NOCACHE, "true" );

		String auditStrategy = getAuditStrategy();
		if ( auditStrategy != null && !"".equals( auditStrategy ) ) {
			config.setProperty( EnversSettings.AUDIT_STRATEGY, auditStrategy );
		}
		config.setProperty( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, "false" );
		// These tests always use H2, so we reset the init_sql config here
		config.setProperty( "hibernate.connection.init_sql", "" );
		addProperties( config );

		this.initMappings();

		ServiceRegistryUtil.applySettings( config.getProperties() );

		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( config.getProperties() );
		sessionFactory = config.buildSessionFactory( serviceRegistry );
	}

	protected abstract void initMappings() throws MappingException, URISyntaxException;

	protected void addProperties(Configuration configuration) {
	}

	protected String getHibernateConfigurationFileName() {
		return "hibernate.test.session-cfg.xml";
	}

	private SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	@AfterClassOnce
	public void closeSessionFactory() {
		try {
			sessionFactory.close();
		}
		finally {
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
				serviceRegistry = null;
			}
		}
	}

	/**
	 * Creates a new session and auditReader only if there is nothing created
	 * before
	 */
	@Before
	public void initializeSession() {
		if ( getSession() == null ) {
			session = getSessionFactory().openSession();
			auditReader = AuditReaderFactory.get( session );
		}
	}

	/**
	 * Creates a new session and auditReader.
	 */
	public void forceNewSession() {
		session = getSessionFactory().openSession();
		auditReader = AuditReaderFactory.get( session );
	}

	protected Session getSession() {
		return session;
	}


	protected AuditReader getAuditReader() {
		return auditReader;
	}


}
