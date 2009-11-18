/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or it's affiliates, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.junit.functional.ExecutionEnvironment;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.transaction.CMTTransactionFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * AbstractDualNodeTestCase.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class DualNodeTestCase extends FunctionalTestCase {
   
   private static final Log log = LogFactory.getLog(DualNodeTestCase.class);
   public static final String NODE_ID_PROP = "hibernate.test.cluster.node.id";
   public static final String LOCAL = "local";
   public static final String REMOTE = "remote";
   private ExecutionEnvironment secondNodeEnvironment;
   private Session secondNodeSession;

   public DualNodeTestCase(String string) {
      super(string);
   }
   
   public String[] getMappings() {
      return new String[] { "cache/infinispan/functional/Contact.hbm.xml", "cache/infinispan/functional/Customer.hbm.xml" };
   }
   
   @Override
   public String getCacheConcurrencyStrategy() {
      return "transactional";
   }
   
   protected Class getCacheRegionFactory() {
      return ClusterAwareRegionFactory.class;
   }

   @Override
   public void configure(Configuration cfg) {
      standardConfigure(cfg);
      configureFirstNode(cfg);
   }

   @Override
   protected void prepareTest() throws Exception {
      log.info("Building second node locally managed execution env");
      secondNodeEnvironment = new ExecutionEnvironment(new SecondNodeSettings());
      secondNodeEnvironment.initialize();
      super.prepareTest();
   }
   
   @Override
   protected void runTest() throws Throwable {
      try {
          super.runTest();
      } finally {
         if ( secondNodeSession != null && secondNodeSession.isOpen() ) {
             if ( secondNodeSession.isConnected() ) {
                secondNodeSession.connection().rollback();
             }
             secondNodeSession.close();
             secondNodeSession = null;
             fail( "unclosed session" );
         } else {
            secondNodeSession = null;
         }
         
      }
   }

   @Override
   protected void cleanupTest() throws Exception {
      try {
          super.cleanupTest();
      
          log.info( "Destroying second node locally managed execution env" );
          secondNodeEnvironment.complete();
          secondNodeEnvironment = null;
      } finally {
         cleanupTransactionManagement();
      }
   }
   
   protected void cleanupTransactionManagement() {
      DualNodeJtaTransactionManagerImpl.cleanupTransactions();
      DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
   }

   public ExecutionEnvironment getSecondNodeEnvironment() {
      return secondNodeEnvironment;
   }

   protected Class getConnectionProviderClass() {
      return DualNodeConnectionProviderImpl.class;
   }

   protected Class getTransactionManagerLookupClass() {
      return DualNodeTransactionManagerLookup.class;
   }

   protected Class getTransactionFactoryClass() {
      return CMTTransactionFactory.class;
   }

   /**
    * Apply any node-specific configurations to our first node.
    * 
    * @param the
    *           Configuration to update.
    */
   protected void configureFirstNode(Configuration cfg) {
      cfg.setProperty(NODE_ID_PROP, LOCAL);
   }

   /**
    * Apply any node-specific configurations to our second node.
    * 
    * @param the
    *           Configuration to update.
    */
   protected void configureSecondNode(Configuration cfg) {
      cfg.setProperty(NODE_ID_PROP, REMOTE);
   }
   
   protected void sleep(long ms) {
      try {
          Thread.sleep(ms);
      }
      catch (InterruptedException e) {
          log.warn("Interrupted during sleep", e);
      }
  }

   protected boolean getUseQueryCache() {
      return true;
   }

   protected void standardConfigure(Configuration cfg) {
      super.configure(cfg);

      cfg.setProperty(Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName());
      cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, getTransactionManagerLookupClass().getName());
      cfg.setProperty(Environment.TRANSACTION_STRATEGY, getTransactionFactoryClass().getName());
      cfg.setProperty(Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName());
      cfg.setProperty(Environment.USE_QUERY_CACHE, String.valueOf(getUseQueryCache()));
   }

   /**
    * Settings impl that delegates most calls to the DualNodeTestCase itself, but overrides the
    * configure method to allow separate cache settings for the second node.
    */
   public class SecondNodeSettings implements ExecutionEnvironment.Settings {
      private final DualNodeTestCase delegate;

      public SecondNodeSettings() {
         this.delegate = DualNodeTestCase.this;
      }

      /**
       * This is the important one -- we extend the delegate's work by adding second-node specific
       * settings
       */
      public void configure(Configuration arg0) {
         delegate.standardConfigure(arg0);
         configureSecondNode(arg0);
      }

      /**
       * Disable creating of schemas; we let the primary session factory do that to our shared
       * database.
       */
      public boolean createSchema() {
         return false;
      }

      /**
       * Disable creating of schemas; we let the primary session factory do that to our shared
       * database.
       */
      public boolean recreateSchemaAfterFailure() {
         return false;
      }

      public void afterConfigurationBuilt(Mappings arg0, Dialect arg1) {
         delegate.afterConfigurationBuilt(arg0, arg1);
      }

      public void afterSessionFactoryBuilt(SessionFactoryImplementor arg0) {
         delegate.afterSessionFactoryBuilt(arg0);
      }

      public boolean appliesTo(Dialect arg0) {
         return delegate.appliesTo(arg0);
      }

      public String getBaseForMappings() {
         return delegate.getBaseForMappings();
      }

      public String getCacheConcurrencyStrategy() {
         return delegate.getCacheConcurrencyStrategy();
      }

      public String[] getMappings() {
         return delegate.getMappings();
      }

      public boolean overrideCacheStrategy() {
         return delegate.overrideCacheStrategy();
      }
   }

}
