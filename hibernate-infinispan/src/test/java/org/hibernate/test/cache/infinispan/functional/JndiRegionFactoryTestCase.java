package org.hibernate.test.cache.infinispan.functional;

import org.hibernate.Session;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.JndiInfinispanRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.testing.junit.functional.ExecutionEnvironment;
import org.hibernate.stat.Statistics;
import org.infinispan.Cache;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.jboss.util.naming.NonSerializableFactory;
import org.jnp.server.Main;
import org.jnp.server.SingletonNamingServer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.util.Properties;

/**
 * // TODO: Document this
 *
 * @author Galder Zamarreño
 * @since // TODO
 */
public class JndiRegionFactoryTestCase extends SingleNodeTestCase {
   private static final Log log = LogFactory.getLog(JndiRegionFactoryTestCase.class);
   private static final String JNDI_NAME = "java:CacheManager";
   private Main namingMain;
   private SingletonNamingServer namingServer;
   private Properties props;
   private boolean bindToJndi = true;
   private EmbeddedCacheManager manager;

   public JndiRegionFactoryTestCase(String string) {
      super(string);
   }

   @Override
   protected void cleanupTest() throws Exception {
      Context ctx = new InitialContext(props);
      unbind(JNDI_NAME, ctx);
      namingServer.destroy();
      namingMain.stop();
      manager.stop(); // Need to stop cos JNDI region factory does not stop it.
   }

   @Override
   protected Class<? extends RegionFactory> getCacheRegionFactory() {
      return JndiInfinispanRegionFactory.class;
   }

   @Override
   public void afterConfigurationBuilt(Mappings mappings, Dialect dialect) {
      if (bindToJndi) {
         try {
            // Create an in-memory jndi
            namingServer = new SingletonNamingServer();
            namingMain = new Main();
            namingMain.setInstallGlobalService(true);
            namingMain.setPort(-1);
            namingMain.start();
            props = new Properties();
            props.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
            props.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");

            manager = new DefaultCacheManager(InfinispanRegionFactory.DEF_INFINISPAN_CONFIG_RESOURCE, false);
            Context ctx = new InitialContext(props);
            bind(JNDI_NAME, manager, EmbeddedCacheManager.class, ctx);
         } catch(Exception e) {
            throw new RuntimeException("Failure to set up JNDI", e);
         }
      }
   }

   @Override
   public void configure(Configuration cfg) {
      super.configure(cfg);
      cfg.setProperty(JndiInfinispanRegionFactory.CACHE_MANAGER_RESOURCE_PROP, JNDI_NAME);
      cfg.setProperty(Environment.JNDI_CLASS, "org.jnp.interfaces.NamingContextFactory");
      cfg.setProperty("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
   }

   public void testRedeployment() throws Exception {
      addEntityCheckCache();
      getEnvironment().getSessionFactory().close();
      bindToJndi = false;
      ExecutionEnvironment environment = new ExecutionEnvironment( this );
      environment.initialize();
      setEnvironment(environment);
      addEntityCheckCache();
      JndiInfinispanRegionFactory regionFactory = (JndiInfinispanRegionFactory) ((SessionFactoryImplementor)
            environment.getSessionFactory()).getSettings().getRegionFactory();
      Cache cache = regionFactory.getCacheManager().getCache("org.hibernate.test.cache.infinispan.functional.Item");
      assertEquals(ComponentStatus.RUNNING, cache.getStatus());
   }

   private void addEntityCheckCache() throws Exception {
      Item item = new Item("chris", "Chris's Item");
      beginTx();
      try {
         Session s = openSession();
         s.getTransaction().begin();
         s.persist(item);
         s.getTransaction().commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      beginTx();
      try {
         Session s = openSession();
         Item found = (Item) s.load(Item.class, item.getId());
         Statistics stats = s.getSessionFactory().getStatistics();
         log.info(stats.toString());
         assertEquals(item.getDescription(), found.getDescription());
         assertEquals(0, stats.getSecondLevelCacheMissCount());
         assertEquals(1, stats.getSecondLevelCacheHitCount());
         s.delete(found);
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
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
      NonSerializableFactory.bind(jndiName, who);
      Name n = ctx.getNameParser("").parse(jndiName);
      while (n.size() > 1) {
         String ctxName = n.get(0);
         try {
            ctx = (Context) ctx.lookup(ctxName);
         } catch (NameNotFoundException e) {
            log.debug("creating Subcontext " + ctxName);
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
//      ctx.unbind(jndiName);
   }

}
