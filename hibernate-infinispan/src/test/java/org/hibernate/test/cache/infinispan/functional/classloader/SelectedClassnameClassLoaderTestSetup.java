/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.cache.infinispan.functional.classloader;
import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * A TestSetup that makes SelectedClassnameClassLoader the thread context classloader for the
 * duration of the test.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class SelectedClassnameClassLoaderTestSetup extends TestSetup {
   private ClassLoader originalTCCL;
   private String[] includedClasses;
   private String[] excludedClasses;
   private String[] notFoundClasses;

   /**
    * Create a new SelectedClassnameClassLoaderTestSetup.
    * 
    * @param test
    */
   public SelectedClassnameClassLoaderTestSetup(Test test, String[] includedClasses, String[] excludedClasses, String[] notFoundClasses) {
      super(test);
      this.includedClasses = includedClasses;
      this.excludedClasses = excludedClasses;
      this.notFoundClasses = notFoundClasses;
   }

   @Override
   protected void setUp() throws Exception {
      super.setUp();

      originalTCCL = Thread.currentThread().getContextClassLoader();
      ClassLoader parent = originalTCCL == null ? getClass().getClassLoader() : originalTCCL;
      ClassLoader selectedTCCL = new SelectedClassnameClassLoader(includedClasses, excludedClasses, notFoundClasses, parent);
      Thread.currentThread().setContextClassLoader(selectedTCCL);
   }

   @Override
   protected void tearDown() throws Exception {
      Thread.currentThread().setContextClassLoader(originalTCCL);
      super.tearDown();
   }

}
