/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.ServiceRegistryBuilder;
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

		String auditStrategy = getAuditStrategy();
		if ( auditStrategy != null && !"".equals( auditStrategy ) ) {
			config.setProperty( EnversSettings.AUDIT_STRATEGY, auditStrategy );
		}
		config.setProperty( Environment.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		config.setProperty( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, "false" );
		addProperties( config );

		this.initMappings();

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
	 * beforeQuery
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
