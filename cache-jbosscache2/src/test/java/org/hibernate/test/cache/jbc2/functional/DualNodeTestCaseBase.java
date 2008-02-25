/*
 * Copyright (c) 2008, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Brian Stansberry
 */

package org.hibernate.test.cache.jbc2.functional;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.junit.functional.ExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for tests that need to create two separate SessionFactory
 * instances to simulate a two-node cluster.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public abstract class DualNodeTestCaseBase extends CacheTestCaseBase
{
   private static final Logger log = LoggerFactory.getLogger( CacheTestCaseBase.class );
   
   private ExecutionEnvironment secondNodeEnvironment;
   private org.hibernate.classic.Session secondNodeSession;
   
   /**
    * Create a new DualNodeTestCaseBase.
    * 
    * @param x
    */
   public DualNodeTestCaseBase(String x)
   {
      super(x);
   }

   @Override
   public void configure(Configuration cfg)
   {
      standardConfigure(cfg);
      configureFirstNode(cfg);
   }    
   
   private void standardConfigure(Configuration cfg) {
      super.configure(cfg);
   }

   /**
    * Apply any node-specific configurations to our first node.
    * 
    * @param the Configuration to update.
    */
   protected abstract void configureFirstNode(Configuration cfg);    

   /**
    * Apply any node-specific configurations to our second node.
    * 
    * @param the Configuration to update.
    */
   protected abstract void configureSecondNode(Configuration cfg);

   @Override
   protected void prepareTest() throws Exception
   {
      log.info( "Building second node locally managed execution env" );
      secondNodeEnvironment = new ExecutionEnvironment( new SecondNodeSettings() );
      secondNodeEnvironment.initialize();
      
      super.prepareTest();
   }
   
   @Override
   protected void runTest() throws Throwable
   {
      try {
          super.runTest();
      }
      finally {

         if ( secondNodeSession != null && secondNodeSession.isOpen() ) {
             if ( secondNodeSession.isConnected() ) {
                secondNodeSession.connection().rollback();
             }
             secondNodeSession.close();
             secondNodeSession = null;
             fail( "unclosed session" );
         }
         else {
            secondNodeSession = null;
         }
         
      }
   }

   @Override
   protected void cleanupTest() throws Exception
   {
      super.cleanupTest();
      
      log.info( "Destroying second node locally managed execution env" );
      secondNodeEnvironment.complete();
      secondNodeEnvironment = null;
   }

   public ExecutionEnvironment getSecondNodeEnvironment() {
      return secondNodeEnvironment;
   }

   /**
    * Settings impl that delegates most calls to the DualNodeTestCase itself,
    * but overrides the configure method to allow separate cache settings
    * for the second node. 
    */
   public class SecondNodeSettings implements ExecutionEnvironment.Settings {
      
      private DualNodeTestCaseBase delegate;
      
      public SecondNodeSettings() {
          this.delegate = DualNodeTestCaseBase.this;
      }

      /**
       * This is the important one -- we extend the delegate's work by
       * adding second-node specific settings
       */
      public void configure(Configuration arg0)
      {
         delegate.standardConfigure(arg0);
         configureSecondNode(arg0);         
      }

      /**
       * Disable creating of schemas; we let the primary session factory
       * do that to our shared database.
       */
      public boolean createSchema()
      {
         return false;
      }

      /**
       * Disable creating of schemas; we let the primary session factory
       * do that to our shared database.
       */
      public boolean recreateSchemaAfterFailure()
      {
         return false;
      }

      public void afterConfigurationBuilt(Mappings arg0, Dialect arg1)
      {
         delegate.afterConfigurationBuilt(arg0, arg1);         
      }

      public void afterSessionFactoryBuilt(SessionFactoryImplementor arg0)
      {
         delegate.afterSessionFactoryBuilt(arg0);
      }

      public boolean appliesTo(Dialect arg0)
      {
         return delegate.appliesTo(arg0);
      }

      public String getBaseForMappings()
      {
         return delegate.getBaseForMappings();
      }

      public String getCacheConcurrencyStrategy()
      {
         return delegate.getCacheConcurrencyStrategy();
      }

      public String[] getMappings()
      {
         return delegate.getMappings();
      }

      public boolean overrideCacheStrategy()
      {
         return delegate.overrideCacheStrategy();
      }
   }

}
