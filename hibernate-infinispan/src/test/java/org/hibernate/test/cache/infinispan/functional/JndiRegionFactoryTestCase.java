/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.util.Map;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.JndiInfinispanRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;

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
public class JndiRegionFactoryTestCase extends SingleNodeTestCase {
	private static final Log log = LogFactory.getLog( JndiRegionFactoryTestCase.class );
	private static final String JNDI_NAME = "java:CacheManager";
	private Main namingMain;
	private SingletonNamingServer namingServer;
	private Properties props;
	private boolean bindToJndi = true;
	private EmbeddedCacheManager manager;

	@Override
	protected void cleanupTest() throws Exception {
		Context ctx = new InitialContext( props );
		unbind( JNDI_NAME, ctx );
		namingServer.destroy();
		namingMain.stop();
		manager.stop(); // Need to stop cos JNDI region factory does not stop it.
	}

	@Override
	protected Class<? extends RegionFactory> getCacheRegionFactory() {
		return JndiInfinispanRegionFactory.class;
	}

	@Override
	protected void afterStandardServiceRegistryBuilt(StandardServiceRegistry ssr) {
		if ( bindToJndi ) {
			try {
				// Create an in-memory jndi
				namingServer = new SingletonNamingServer();
				namingMain = new Main();
				namingMain.setInstallGlobalService( true );
				namingMain.setPort( -1 );
				namingMain.start();
				props = new Properties();
				props.put( "java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory" );
				props.put( "java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces" );

				final String cfgFileName = (String) ssr.getService( ConfigurationService.class ).getSettings().get(
						InfinispanRegionFactory.INFINISPAN_CONFIG_RESOURCE_PROP
				);
				manager = new DefaultCacheManager(
						cfgFileName == null ? InfinispanRegionFactory.DEF_INFINISPAN_CONFIG_RESOURCE : cfgFileName,
						false
				);
				Context ctx = new InitialContext( props );
				bind( JNDI_NAME, manager, EmbeddedCacheManager.class, ctx );
			}
			catch (Exception e) {
				throw new RuntimeException( "Failure to set up JNDI", e );
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( JndiInfinispanRegionFactory.CACHE_MANAGER_RESOURCE_PROP, JNDI_NAME );
		settings.put( Environment.JNDI_CLASS, "org.jnp.interfaces.NamingContextFactory" );
		settings.put( "java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces" );
	}

	@Test
	public void testRedeployment() throws Exception {
		addEntityCheckCache( sessionFactory() );
		bindToJndi = false;
		rebuildSessionFactory();

		addEntityCheckCache( sessionFactory() );
		JndiInfinispanRegionFactory regionFactory = (JndiInfinispanRegionFactory) sessionFactory().getSettings().getRegionFactory();
		Cache cache = regionFactory.getCacheManager().getCache( "org.hibernate.test.cache.infinispan.functional.Item" );
		assertEquals( ComponentStatus.RUNNING, cache.getStatus() );
	}

	private void addEntityCheckCache(SessionFactoryImplementor sessionFactory) throws Exception {
		Item item = new Item( "chris", "Chris's Item" );
		beginTx();
		try {
			Session s = sessionFactory.openSession();
			s.getTransaction().begin();
			s.persist( item );
			s.getTransaction().commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		beginTx();
		try {
			Session s =	sessionFactory.openSession();
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
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

	}

	/**
	 * Helper method that binds the a non serializable object to the JNDI tree.
	 *
	 * @param jndiName Name under which the object must be bound
	 * @param who Object to bind in JNDI
	 * @param classType Class type under which should appear the bound object
	 * @param ctx Naming context under which we bind the object
	 * @throws Exception Thrown if a naming exception occurs during binding
	 */
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

	private void unbind(String jndiName, Context ctx) throws Exception {
		NonSerializableFactory.unbind( jndiName );
//      ctx.unbind(jndiName);
	}

}
