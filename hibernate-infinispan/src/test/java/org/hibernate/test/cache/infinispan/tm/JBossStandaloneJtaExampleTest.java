/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
package org.hibernate.test.cache.infinispan.tm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Iterator;
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
import org.hibernate.TruthValue;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.stat.Statistics;
import org.hibernate.test.cache.infinispan.functional.Item;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.jta.JtaAwareConnectionProviderImpl;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.jboss.util.naming.NonSerializableFactory;
import org.jnp.interfaces.NamingContext;
import org.jnp.server.Main;
import org.jnp.server.NamingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
   private static final Log log = LogFactory.getLog(JBossStandaloneJtaExampleTest.class);
   private static final JBossStandaloneJTAManagerLookup lookup = new JBossStandaloneJTAManagerLookup();
   Context ctx;
   Main jndiServer;
   private ServiceRegistry serviceRegistry;

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
            session.getTransaction().begin();
            item = new Item("anItem", "An item owned by someone");
            session.persist(item);
            session.getTransaction().commit();
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
            session.getTransaction().begin();
            Item found = (Item) session.load(Item.class, item.getId());
            Statistics stats = session.getSessionFactory().getStatistics();
            log.info(stats.toString());
            assertEquals(item.getDescription(), found.getDescription());
            assertEquals(0, stats.getSecondLevelCacheMissCount());
            assertEquals(1, stats.getSecondLevelCacheHitCount());
            session.delete(found);
            session.getTransaction().commit();
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
            session.getTransaction().begin();
            assertNull(session.get(Item.class, item.getId()));
            session.getTransaction().commit();
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
	  Properties envProps = Environment.getProperties();
	  envProps.setProperty( Environment.DIALECT, "HSQL" );
      envProps.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
      envProps.setProperty( Environment.CONNECTION_PROVIDER, JtaAwareConnectionProviderImpl.class.getName() );
      envProps.setProperty(Environment.JNDI_CLASS, "org.jnp.interfaces.NamingContextFactory");
      envProps.setProperty(Environment.TRANSACTION_STRATEGY, "jta");
      envProps.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "jta");
      envProps.setProperty(Environment.RELEASE_CONNECTIONS, "auto");
      envProps.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
      envProps.setProperty(Environment.USE_QUERY_CACHE, "true");
      envProps.put(AvailableSettings.JTA_PLATFORM, new JBossStandAloneJtaPlatform());
      envProps.setProperty(Environment.CACHE_REGION_FACTORY,
              "org.hibernate.test.cache.infinispan.functional.SingleNodeTestCase$TestInfinispanRegionFactory");
      serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry(envProps);
	  
      MetadataSources sources = new MetadataSources( serviceRegistry );

      String[] mappings = new String[]{"org/hibernate/test/cache/infinispan/functional/Item.hbm.xml"};
      for (String mapping : mappings) {
    	  sources.addResource(mapping);
      }
      Metadata metadata = sources.buildMetadata();
      Iterator<EntityBinding> entityIter = metadata.getEntityBindings().iterator();
      while (entityIter.hasNext()) {
         EntityBinding binding = entityIter.next();
         binding.getHierarchyDetails().getCaching().setAccessType( AccessType.TRANSACTIONAL );
         binding.getHierarchyDetails().getCaching().setRequested( TruthValue.TRUE );
         binding.getHierarchyDetails().getCaching().setRegion( binding.getEntityName() );
         binding.getHierarchyDetails().getCaching().setCacheLazyProperties( true );
      }
      Iterator<PluralAttributeBinding> collectionIter = metadata.getCollectionBindings().iterator();
      while (collectionIter.hasNext()) {
    	 PluralAttributeBinding binding = collectionIter.next();
         binding.getCaching().setAccessType( AccessType.TRANSACTIONAL );
         binding.getCaching().setRequested( TruthValue.TRUE );
         binding.getCaching().setRegion( StringHelper.qualify(
        		 binding.getContainer().seekEntityBinding().getEntityName(), binding.getAttribute().getName() ) );
         binding.getCaching().setCacheLazyProperties( true );
      }
      return metadata.buildSessionFactory();
   }
}
