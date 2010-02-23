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

import junit.framework.TestCase;
import org.enhydra.jdbc.standard.StandardXADataSource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.stat.Statistics;
import org.hibernate.test.cache.infinispan.functional.Item;
import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.jboss.util.naming.NonSerializableFactory;
import org.jnp.interfaces.NamingContext;
import org.jnp.server.Main;
import org.jnp.server.NamingServer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;

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
public class JBossStandaloneJtaExampleTest extends TestCase {
   private static final Log log = LogFactory.getLog(JBossStandaloneJtaExampleTest.class);
   private static final JBossStandaloneJTAManagerLookup lookup = new JBossStandaloneJTAManagerLookup();
   Context ctx;
   Main jndiServer;

   @Override
   protected void setUp() throws Exception {
      super.setUp();
      jndiServer = startJndiServer();
      ctx = createJndiContext();
      bindTransactionManager();
      bindUserTransaction();
      bindDataSource();
   }

   @Override
   protected void tearDown() throws Exception {
      super.tearDown();
      ctx.close();
      jndiServer.stop();
   }

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

   public static class ExtendedXADataSource extends StandardXADataSource { // XAPOOL
      @Override
      public Connection getConnection() throws SQLException {

         if (getTransactionManager() == null) { // although already set before, it results null again after retrieving the datasource by jndi
            TransactionManager tm;  // this is because the TransactionManager information is not serialized.
            try {
               tm = lookup.getTransactionManager();
            } catch (Exception e) {
               throw new SQLException(e);
            }
            setTransactionManager(tm);  //  resets the TransactionManager on the datasource retrieved by jndi,
            //  this makes the datasource JTA-aware
         }

         // According to Enhydra documentation, here we must return the connection of our XAConnection
         // see http://cvs.forge.objectweb.org/cgi-bin/viewcvs.cgi/xapool/xapool/examples/xapooldatasource/DatabaseHelper.java?sortby=rev
         return super.getXAConnection().getConnection();
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
         return null;  // JDK6 stuff
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) throws SQLException {
         return false;  // JDK6 stuff
      }
   }

   private Main startJndiServer() throws Exception {
      // Create an in-memory jndi
      NamingServer namingServer = new NamingServer();
      NamingContext.setLocal(namingServer);
      Main namingMain = new Main();
      namingMain.setInstallGlobalService(true);
      namingMain.setPort(-1);
      namingMain.start();
      return namingMain;
   }

   private Context createJndiContext() throws Exception {
      Properties props = new Properties();
      props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
      props.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
      return new InitialContext(props);
   }

   private void bindTransactionManager() throws Exception {
      // as JBossTransactionManagerLookup extends JNDITransactionManagerLookup we must also register the TransactionManager
      bind("java:/TransactionManager", lookup.getTransactionManager(), lookup.getTransactionManager().getClass(), ctx);
   }

   private void bindUserTransaction() throws Exception {
      // also the UserTransaction must be registered on jndi: org.hibernate.transaction.JTATransactionFactory#getUserTransaction() requires this
      bind("UserTransaction", lookup.getUserTransaction(), lookup.getUserTransaction().getClass(), ctx);
   }

   private void bindDataSource() throws Exception {
      ExtendedXADataSource xads = new ExtendedXADataSource();
      xads.setDriverName("org.hsqldb.jdbcDriver");
      xads.setUrl("jdbc:hsqldb:mem:/test");
      ctx.bind("java:/MyDatasource", xads);
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
      Configuration cfg = new Configuration();
      cfg.setProperty(Environment.DIALECT, "org.hibernate.dialect.HSQLDialect");
      cfg.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
      cfg.setProperty(Environment.DATASOURCE, "java:/MyDatasource");
      cfg.setProperty(Environment.JNDI_CLASS, "org.jnp.interfaces.NamingContextFactory");
      cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, "org.hibernate.transaction.JBossTransactionManagerLookup");
      cfg.setProperty(Environment.TRANSACTION_STRATEGY, "org.hibernate.transaction.JTATransactionFactory");
      cfg.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "jta");
      cfg.setProperty(Environment.RELEASE_CONNECTIONS, "auto");
      cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
      cfg.setProperty(Environment.USE_QUERY_CACHE, "true");
      cfg.setProperty(Environment.CACHE_REGION_FACTORY, "org.hibernate.cache.infinispan.InfinispanRegionFactory");
      String[] mappings = new String[]{"org/hibernate/test/cache/infinispan/functional/Item.hbm.xml"};
      for (String mapping : mappings) {
         cfg.addResource(mapping, Thread.currentThread().getContextClassLoader());
      }
      Iterator iter = cfg.getClassMappings();
      while (iter.hasNext()) {
         PersistentClass clazz = (PersistentClass) iter.next();
         cfg.setCacheConcurrencyStrategy(clazz.getEntityName(), "transactional");
      }
      iter = cfg.getCollectionMappings();
      while (iter.hasNext()) {
         Collection coll = (Collection) iter.next();
         cfg.setCollectionCacheConcurrencyStrategy(coll.getRole(), "transactional");
      }
      return cfg.buildSessionFactory();
   }
}
