/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.junit4;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.HashtableCacheProvider;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.service.internal.ServiceRegistryImpl;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.OnExpectedFailure;
import org.hibernate.testing.OnFailure;
import org.hibernate.testing.SkipLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Blob;
import java.sql.Clob;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.fail;

/**
 * Applies functional testing logic for core Hibernate testing on top of {@link BaseUnitTestCase}
 *
 * @author Steve Ebersole
 */
public abstract class BaseCoreFunctionalTestCase extends BaseUnitTestCase {
	public static final String VALIDATE_DATA_CLEANUP = "hibernate.test.validateDataCleanup";
	public static final Dialect DIALECT = Dialect.getDialect();

	private static final Logger log = LoggerFactory.getLogger( BaseCoreFunctionalTestCase.class );

	private static Configuration configuration;
	private static ServiceRegistryImpl serviceRegistry;
	private static SessionFactoryImplementor sessionFactory;

	private org.hibernate.classic.Session session;

	protected Dialect getDialect() {
		return DIALECT;
	}

	protected Configuration configuration() {
		return configuration;
	}

	@Deprecated
	protected Configuration getCfg() {
		// todo : remove.  this is legacy.  convert usages to configuration()
		return configuration();
	}

	protected ServiceRegistryImpl serviceRegistry() {
		return serviceRegistry;
	}

	@Deprecated
	protected SessionFactoryImplementor getSessionFactory() {
		// todo : remove.  this is legacy.  convert usages to sessionFactory()
		return sessionFactory();
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	@Deprecated
	protected SessionFactory getSessions() {
		// todo : remove.  this is legacy.  convert usages to sessionFactory()
		return sessionFactory();
	}

	@Deprecated
	protected SessionFactoryImplementor sfi() {
		// todo : remove.  this is legacy.  convert usages to sessionFactory()
		return sessionFactory();
	}

	protected org.hibernate.classic.Session openSession() throws HibernateException {
		session = sessionFactory().openSession();
		return session;
	}

	protected org.hibernate.classic.Session openSession(Interceptor interceptor) throws HibernateException {
		session = sessionFactory().openSession(interceptor);
		return session;
	}


	// before/after test class ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@BeforeClassOnce
	private void buildSessionFactory() {
		log.trace( "Building session factory" );
		configuration = buildConfiguration();
		serviceRegistry = buildServiceRegistry( configuration );
		sessionFactory = (SessionFactoryImplementor) configuration.buildSessionFactory( serviceRegistry );
		afterSessionFactoryBuilt();
	}

	protected Configuration buildConfiguration() {
		Configuration cfg = constructConfiguration();
		configure( cfg );
		addMappings( cfg );
		cfg.buildMappings();
		applyCacheSettings( cfg );
		afterConfigurationBuilt( cfg );
		return cfg;
	}

	protected Configuration constructConfiguration() {
		Configuration configuration = new Configuration()
				.setProperty( Environment.CACHE_PROVIDER, HashtableCacheProvider.class.getName() );
		if ( createSchema() ) {
			configuration.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		}
		configuration.setProperty( Environment.DIALECT, getDialect().getClass().getName() );
		return configuration;
	}

	protected void configure(Configuration configuration) {
	}

	protected void addMappings(Configuration configuration) {
		String[] mappings = getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				configuration.addResource(
						getBaseForMappings() + mapping,
						getClass().getClassLoader()
				);
			}
		}
	}

	protected static final String[] NO_MAPPINGS = new String[0];

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getAnnotatedPackages() {
		return NO_MAPPINGS;
	}

	protected String[] getXmlFiles() {
		// todo : rename to getOrmXmlFiles()
		return NO_MAPPINGS;
	}

	protected void applyCacheSettings(Configuration configuration) {
		if ( getCacheConcurrencyStrategy() != null ) {
			Iterator itr = configuration.getClassMappings();
			while ( itr.hasNext() ) {
				PersistentClass clazz = (PersistentClass) itr.next();
				Iterator props = clazz.getPropertyClosureIterator();
				boolean hasLob = false;
				while ( props.hasNext() ) {
					Property prop = (Property) props.next();
					if ( prop.getValue().isSimpleValue() ) {
						String type = ( (SimpleValue) prop.getValue() ).getTypeName();
						if ( "blob".equals(type) || "clob".equals(type) ) {
							hasLob = true;
						}
						if ( Blob.class.getName().equals(type) || Clob.class.getName().equals(type) ) {
							hasLob = true;
						}
					}
				}
				if ( !hasLob && !clazz.isInherited() && overrideCacheStrategy() ) {
					configuration.setCacheConcurrencyStrategy( clazz.getEntityName(), getCacheConcurrencyStrategy() );
				}
			}
			itr = configuration.getCollectionMappings();
			while ( itr.hasNext() ) {
				Collection coll = (Collection) itr.next();
				configuration.setCollectionCacheConcurrencyStrategy( coll.getRole(), getCacheConcurrencyStrategy() );
			}
		}
	}

	protected boolean overrideCacheStrategy() {
		return true;
	}

	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	protected void afterConfigurationBuilt(Configuration configuration) {
		afterConfigurationBuilt( configuration.createMappings(), getDialect() );
	}

	protected void afterConfigurationBuilt(Mappings mappings, Dialect dialect) {
	}

	protected ServiceRegistryImpl buildServiceRegistry(Configuration configuration) {
		Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		ServiceRegistryImpl serviceRegistry = new ServiceRegistryImpl( properties );
		applyServices( serviceRegistry );
		return serviceRegistry;
	}

	protected void applyServices(ServiceRegistryImpl serviceRegistry) {
	}

	protected void afterSessionFactoryBuilt() {
	}

	protected boolean createSchema() {
		return true;
	}

	protected boolean rebuildSessionFactoryOnError() {
		return true;
	}

	@AfterClassOnce
	private void releaseSessionFactory() {
		if ( sessionFactory == null ) {
			return;
		}
		log.trace( "Releasing session factory" );
		sessionFactory.close();
		sessionFactory = null;
		configuration = null;
	}

	@OnFailure
	@OnExpectedFailure
	public void onFailure() {
		log.trace( "Processing failure-expected ignore" );
		if ( ! rebuildSessionFactoryOnError() ) {
			return;
		}

		rebuildSessionFactory();
	}

	protected void rebuildSessionFactory() {
		if ( sessionFactory == null ) {
			return;
		}
		sessionFactory.close();
		serviceRegistry.destroy();

		serviceRegistry = buildServiceRegistry( configuration );
		sessionFactory = (SessionFactoryImplementor) configuration.buildSessionFactory( serviceRegistry );
		afterSessionFactoryBuilt();
	}


	// before/after each test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Before
	public final void beforeTest() throws Exception {
		prepareTest();
	}

	protected void prepareTest() throws Exception {
	}

	@After
	public final void afterTest() throws Exception {
		cleanupTest();

		if ( session != null && session.isOpen() ) {
			if ( session.isConnected() ) {
				session.connection().rollback();
			}
			session.close();
			session = null;
			log.debug( "unclosed session" );
		}
		else {
			session = null;
		}

		assertAllDataRemoved();
	}

	protected void cleanupTest() throws Exception {
	}

	@SuppressWarnings( {"UnnecessaryBoxing", "UnnecessaryUnboxing"})
	protected void assertAllDataRemoved() {
		if ( !createSchema() ) {
			return; // no tables were created...
		}
		if ( !Boolean.getBoolean( VALIDATE_DATA_CLEANUP ) ) {
			return;
		}

		Session tmpSession = sessionFactory.openSession();
		try {
			List list = tmpSession.createQuery( "select o from java.lang.Object o" ).list();

			Map<String,Integer> items = new HashMap<String,Integer>();
			if ( !list.isEmpty() ) {
				for ( Object element : list ) {
					Integer l = (Integer) items.get( tmpSession.getEntityName( element ) );
					if ( l == null ) {
						l = Integer.valueOf( 0 );
					}
					l = Integer.valueOf( l.intValue() + 1 ) ;
					items.put( tmpSession.getEntityName( element ), l );
					System.out.println( "Data left: " + element );
				}
				fail( "Data is left in the database: " + items.toString() );
			}
		}
		finally {
			try {
				tmpSession.close();
			}
			catch( Throwable t ) {
				// intentionally empty
			}
		}
	}

	protected boolean readCommittedIsolationMaintained(String scenario) {
		int isolation = java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
		Session testSession = null;
		try {
			testSession = openSession();
			isolation = testSession.connection().getTransactionIsolation();
		}
		catch( Throwable ignore ) {
		}
		finally {
			if ( testSession != null ) {
				try {
					testSession.close();
				}
				catch( Throwable ignore ) {
				}
			}
		}
		if ( isolation < java.sql.Connection.TRANSACTION_READ_COMMITTED ) {
			SkipLog.reportSkip( "environment does not support at least read committed isolation", scenario );
			return false;
		}
		else {
			return true;
		}
	}

}
