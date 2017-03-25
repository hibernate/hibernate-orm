/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.tm;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.stat.Statistics;

import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.jta.JtaAwareConnectionProviderImpl;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;

import org.jboss.util.naming.NonSerializableFactory;

import org.jnp.interfaces.NamingContext;
import org.jnp.server.Main;
import org.jnp.server.NamingServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This is an example test based on http://community.jboss.org/docs/DOC-14617 that shows how to interact with
 * Hibernate configured with Infinispan second level cache provider using JTA transactions.
 *
 * In this test, an XADataSource wrapper is in use where we have associated our transaction manager to it so that
 * commits/rollbacks are propagated to the database as well.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class JBossStandaloneJtaExampleTest {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(JBossStandaloneJtaExampleTest.class);
	private static final JBossStandaloneJTAManagerLookup lookup = new JBossStandaloneJTAManagerLookup();
	Context ctx;
	Main jndiServer;
	private ServiceRegistry serviceRegistry;

	@Rule
	public final InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

	@Before
	public void setUp() throws Exception {
		jndiServer = startJndiServer();
		ctx = createJndiContext();
		// Inject configuration to initialise transaction manager from config classloader
		lookup.init(new ConfigurationBuilder().build());
		bindTransactionManager();
		bindUserTransaction();
	}

	@After
	public void tearDown() throws Exception {
		try {
			unbind("UserTransaction", ctx);
			unbind("java:/TransactionManager", ctx);
			ctx.close();
			jndiServer.stop();
	  }
	  finally {
		  if ( serviceRegistry != null ) {
			  ServiceRegistryBuilder.destroy( serviceRegistry );
		  }
	  }
	}
	@Test
	public void testPersistAndLoadUnderJta() throws Exception {
		Item item;
		SessionFactory sessionFactory = buildSessionFactory();
		try {
			UserTransaction ut = (UserTransaction) ctx.lookup("UserTransaction");
			ut.begin();
			try {
				Session session = sessionFactory.openSession();
				assertTrue(session.getTransaction().isActive());
				item = new Item("anItem", "An item owned by someone");
				session.persist(item);
				// IMO the flush should not be necessary, but session.close() does not flush
				// and the item is not persisted.
				session.flush();
				session.close();
			} catch(Exception e) {
				ut.setRollbackOnly();
				throw e;
			} finally {
				if (ut.getStatus() == Status.STATUS_ACTIVE)
					ut.commit();
				else
					ut.rollback();
			}

			ut = (UserTransaction) ctx.lookup("UserTransaction");
			ut.begin();
			try {
				Session session = sessionFactory.openSession();
				assertTrue(session.getTransaction().isActive());
				Item found = (Item) session.load(Item.class, item.getId());
				Statistics stats = session.getSessionFactory().getStatistics();
				log.info(stats.toString());
				assertEquals(item.getDescription(), found.getDescription());
				assertEquals(0, stats.getSecondLevelCacheMissCount());
				assertEquals(1, stats.getSecondLevelCacheHitCount());
				session.delete(found);
				// IMO the flush should not be necessary, but session.close() does not flush
				// and the item is not deleted.
				session.flush();
				session.close();
			} catch(Exception e) {
				ut.setRollbackOnly();
				throw e;
			} finally {
				if (ut.getStatus() == Status.STATUS_ACTIVE)
					ut.commit();
				else
					ut.rollback();
			}

			ut = (UserTransaction) ctx.lookup("UserTransaction");
			ut.begin();
			try {
				Session session = sessionFactory.openSession();
				assertTrue(session.getTransaction().isActive());
				assertNull(session.get(Item.class, item.getId()));
				session.close();
			} catch(Exception e) {
				ut.setRollbackOnly();
				throw e;
			} finally {
				if (ut.getStatus() == Status.STATUS_ACTIVE)
					ut.commit();
				else
					ut.rollback();
			}
		} finally {
			if (sessionFactory != null)
				sessionFactory.close();
		}

	}

	private Main startJndiServer() throws Exception {
		// Create an in-memory jndi
		NamingServer namingServer = new NamingServer();
		NamingContext.setLocal(namingServer);
		Main namingMain = new Main();
		namingMain.setInstallGlobalService(true);
		namingMain.setPort( -1 );
		namingMain.start();
		return namingMain;
	}

	private Context createJndiContext() throws Exception {
		Properties props = new Properties();
		props.put( Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory" );
		props.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
		return new InitialContext(props);
	}

	private void bindTransactionManager() throws Exception {
		// as JBossTransactionManagerLookup extends JNDITransactionManagerLookup we must also register the TransactionManager
		bind("java:/TransactionManager", lookup.getTransactionManager(), lookup.getTransactionManager().getClass(), ctx);
	}

	private void bindUserTransaction() throws Exception {
		// also the UserTransaction must be registered on jndi: org.hibernate.engine.transaction.internal.jta.JtaTransactionFactory#getUserTransaction() requires this
		bind( "UserTransaction", lookup.getUserTransaction(), lookup.getUserTransaction().getClass(), ctx );
	}

	/**
	 * Helper method that binds the a non serializable object to the JNDI tree.
	 *
	 * @param jndiName  Name under which the object must be bound
	 * @param who       Object to bind in JNDI
	 * @param classType Class type under which should appear the bound object
	 * @param ctx       Naming context under which we bind the object
	 * @throws Exception Thrown if a naming exception occurs during binding
	 */
	private void bind(String jndiName, Object who, Class classType, Context ctx) throws Exception {
		// Ah ! This service isn't serializable, so we use a helper class
		NonSerializableFactory.bind(jndiName, who);
		Name n = ctx.getNameParser("").parse(jndiName);
		while (n.size() > 1) {
			String ctxName = n.get(0);
			try {
				ctx = (Context) ctx.lookup(ctxName);
			} catch (NameNotFoundException e) {
				System.out.println("Creating subcontext:" + ctxName);
				ctx = ctx.createSubcontext(ctxName);
			}
			n = n.getSuffix(1);
		}

		// The helper class NonSerializableFactory uses address type nns, we go on to
		// use the helper class to bind the service object in JNDI
		StringRefAddr addr = new StringRefAddr("nns", jndiName);
		Reference ref = new Reference(classType.getName(), addr, NonSerializableFactory.class.getName(), null);
		ctx.rebind(n.get(0), ref);
	}

	private void unbind(String jndiName, Context ctx) throws Exception {
		NonSerializableFactory.unbind(jndiName);
		ctx.unbind(jndiName);
	}

	private SessionFactory buildSessionFactory() {
		// Extra options located in src/test/resources/hibernate.properties
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder()
				  .applySetting( Environment.DIALECT, "HSQL" )
				  .applySetting( Environment.HBM2DDL_AUTO, "create-drop" )
				  .applySetting( Environment.CONNECTION_PROVIDER, JtaAwareConnectionProviderImpl.class.getName() )
				  .applySetting( Environment.JNDI_CLASS, "org.jnp.interfaces.NamingContextFactory" )
				  .applySetting( Environment.TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class.getName() )
				  .applySetting( Environment.CURRENT_SESSION_CONTEXT_CLASS, "jta" )
				  .applySetting( Environment.RELEASE_CONNECTIONS, "auto" )
				  .applySetting( Environment.USE_SECOND_LEVEL_CACHE, "true" )
				  .applySetting( Environment.USE_QUERY_CACHE, "true" )
				  .applySetting( Environment.JTA_PLATFORM, new JBossStandAloneJtaPlatform() )
				  .applySetting( Environment.CACHE_REGION_FACTORY, TestInfinispanRegionFactory.class.getName() );

		StandardServiceRegistry serviceRegistry = ssrb.build();

		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		metadataSources.addResource("org/hibernate/test/cache/infinispan/functional/entities/Item.hbm.xml");

		Metadata metadata = metadataSources.buildMetadata();
		for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
			if ( entityBinding instanceof RootClass ) {
				( (RootClass) entityBinding ).setCacheConcurrencyStrategy( "transactional" );
			}
		}
		for ( Collection collectionBinding : metadata.getCollectionBindings() ) {
			collectionBinding.setCacheConcurrencyStrategy( "transactional" );
		}

		return metadata.buildSessionFactory();
	}
}
