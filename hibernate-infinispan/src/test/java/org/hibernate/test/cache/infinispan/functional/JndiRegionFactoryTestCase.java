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
package org.hibernate.test.cache.infinispan.functional;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Session;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.JndiInfinispanRegionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.infinispan.Cache;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import org.jboss.util.naming.NonSerializableFactory;

import org.jnp.server.Main;
import org.jnp.server.SingletonNamingServer;
import static org.junit.Assert.assertEquals;

/**
 * // TODO: Document this
 *
 * @author Galder Zamarreño
 * @since // TODO
 */
public class JndiRegionFactoryTestCase extends BaseUnitTestCase {
	private static final Log log = LogFactory.getLog( JndiRegionFactoryTestCase.class );

	private static final String JNDI_NAME = "java:CacheManager";

	// Naming
	private Main namingMain;
	private SingletonNamingServer namingServer;
	private EmbeddedCacheManager manager;


	@Before
	public void setUp() {
		try {
			// Create an in-memory jndi
			namingServer = new SingletonNamingServer();
			namingMain = new Main();
			namingMain.setInstallGlobalService( true );
			namingMain.setPort( -1 );
			namingMain.start();

			final Properties props = new Properties();
			props.put( "java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory" );
			props.put( "java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces" );

			manager = new DefaultCacheManager( InfinispanRegionFactory.DEF_INFINISPAN_CONFIG_RESOURCE, false );
			Context ctx = new InitialContext( props );
			bind( JNDI_NAME, manager, EmbeddedCacheManager.class, ctx );
		}
		catch (Exception e) {
			throw new RuntimeException( "Failure to set up JNDI", e );
		}
	}

	private void bind(String jndiName, Object who, Class<?> classType, Context ctx) throws Exception {
		// Ah ! This service isn't serializable, so we use a helper class
		NonSerializableFactory.bind( jndiName, who );
		Name n = ctx.getNameParser( "" ).parse( jndiName );
		while ( n.size() > 1 ) {
			String ctxName = n.get( 0 );
			try {
				ctx = (Context) ctx.lookup( ctxName );
			}
			catch (NameNotFoundException e) {
				log.debug( "creating Subcontext " + ctxName );
				ctx = ctx.createSubcontext( ctxName );
			}
			n = n.getSuffix( 1 );
		}

		// The helper class NonSerializableFactory uses address type nns, we go on to
		// use the helper class to bind the service object in JNDI
		StringRefAddr addr = new StringRefAddr( "nns", jndiName );
		Reference ref = new Reference( classType.getName(), addr, NonSerializableFactory.class.getName(), null );
		ctx.rebind( n.get( 0 ), ref );
	}

	@After
	public void tearDown() {
		try {
			NonSerializableFactory.unbind( JNDI_NAME );
			namingServer.destroy();
			namingMain.stop();
			manager.stop(); // Need to stop cos JNDI region factory does not stop it.
		}
		catch (Exception e) {
			throw new RuntimeException( "Failure to clean up JNDI", e );
		}
	}

	@Test
	public void testRedeployment() throws Exception {
		SessionFactoryImplementor sf = (SessionFactoryImplementor) buildMetadata().buildSessionFactory();
		addEntityCheckCache( sf );
		sf.close();

		sf = (SessionFactoryImplementor) buildMetadata().buildSessionFactory();
		addEntityCheckCache( sf );
		JndiInfinispanRegionFactory regionFactory = (JndiInfinispanRegionFactory) sf.getSettings().getRegionFactory();
		Cache cache = regionFactory.getCacheManager().getCache( "org.hibernate.test.cache.infinispan.functional.Item" );
		assertEquals( ComponentStatus.RUNNING, cache.getStatus() );
	}

	@SuppressWarnings("unchecked")
	private Metadata buildMetadata() {
		Map config = new HashMap();
		config.put( Environment.HBM2DDL_AUTO, "create-drop" );
		config.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		config.put( Environment.GENERATE_STATISTICS, "true" );
		config.put( Environment.USE_QUERY_CACHE, "true" );
		config.put( Environment.CACHE_REGION_FACTORY, JndiInfinispanRegionFactory.class.getName() );
		config.put( JndiInfinispanRegionFactory.CACHE_MANAGER_RESOURCE_PROP, JNDI_NAME );
		config.put( Environment.JNDI_CLASS, "org.jnp.interfaces.NamingContextFactory" );
		config.put( "java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces" );

		TestingJtaBootstrap.prepare( config );
		config.put( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName() );
		config.put( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_STATEMENT.toString() );

		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr ).applySettings( config ).build();

		MetadataSources sources = new MetadataSources( bsr );
		sources.addResource( "org/hibernate/test/cache/infinispan/functional/Item.hbm.xml" );
		sources.addResource( "org/hibernate/test/cache/infinispan/functional/Customer.hbm.xml" );
		sources.addResource( "org/hibernate/test/cache/infinispan/functional/Contact.hbm.xml" );

		MetadataImplementor metadata = (MetadataImplementor) sources.getMetadataBuilder( ssr ).build();
		BaseCoreFunctionalTestCase.overrideCacheSettings( metadata, "transactional" );
		return metadata;
	}

	private void addEntityCheckCache(SessionFactoryImplementor sessionFactory) throws Exception {
		Item item = new Item( "chris", "Chris's Item" );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		try {
			Session s = sessionFactory.openSession();
			s.getTransaction().begin();
			s.persist( item );
			s.getTransaction().commit();
			s.close();
		}
		catch (Exception e) {
			log.error( "Error", e );
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().setRollbackOnly();
		}
		finally {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		try {
			Session s = sessionFactory.openSession();
			Item found = (Item) s.load( Item.class, item.getId() );
			Statistics stats = sessionFactory.getStatistics();
			log.info( stats.toString() );
			assertEquals( item.getDescription(), found.getDescription() );
			assertEquals( 0, stats.getSecondLevelCacheMissCount() );
			assertEquals( 1, stats.getSecondLevelCacheHitCount() );
			s.delete( found );
			s.close();
		}
		catch (Exception e) {
			log.error( "Error", e );
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().setRollbackOnly();
		}
		finally {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}

	}
}
