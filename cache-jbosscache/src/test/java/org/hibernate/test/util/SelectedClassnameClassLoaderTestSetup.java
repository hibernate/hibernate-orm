/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
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

package org.hibernate.test.util;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * A TestSetup that makes SelectedClassnameClassLoader the thread
 * context classloader for the duration of the test.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class SelectedClassnameClassLoaderTestSetup extends TestSetup
{
   private ClassLoader originalTCCL;
   private String[] includedClasses;
   private String[] excludedClasses;
   private String[] notFoundClasses;
   
   
   /**
    * Create a new SelectedClassnameClassLoaderTestSetup.
    * 
    * @param test
    */
   public SelectedClassnameClassLoaderTestSetup(Test test,
                                                String[] includedClasses,
                                                String[] excludedClasses,
                                                String[] notFoundClasses)
   {
      super(test);
      this.includedClasses = includedClasses;
      this.excludedClasses = excludedClasses;
      this.notFoundClasses = notFoundClasses;
   }

   @Override
   protected void setUp() throws Exception
   {      
      super.setUp();
      
      originalTCCL = Thread.currentThread().getContextClassLoader();
      ClassLoader parent = originalTCCL == null ? getClass().getClassLoader() : originalTCCL;
      ClassLoader selectedTCCL = new SelectedClassnameClassLoader(includedClasses, excludedClasses, notFoundClasses, parent);
      Thread.currentThread().setContextClassLoader(selectedTCCL);
   }

   @Override
   protected void tearDown() throws Exception
   {
      Thread.currentThread().setContextClassLoader(originalTCCL);
      super.tearDown();
   }
   
   

}
