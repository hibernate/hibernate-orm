/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.test.cache.infinispan.functional.classloader;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ClassLoader that loads classes whose classname begins with one of a given set of strings,
 * without attempting first to delegate to its parent loader.
 * <p>
 * This class is intended to allow emulation of 2 different types of common J2EE classloading
 * situations.
 * <ul>
 * <li>Servlet-style child-first classloading, where this class is the child loader.</li>
 * <li>Parent-first classloading where the parent does not have access to certain classes</li>
 * </ul>
 * </p>
 * <p>
 * This class can also be configured to raise a ClassNotFoundException if asked to load certain
 * classes, thus allowing classes on the classpath to be hidden from a test environment.
 * </p>
 * 
 * @author Brian Stansberry
 */
public class SelectedClassnameClassLoader extends ClassLoader {
   private Logger log = LoggerFactory.getLogger(SelectedClassnameClassLoader.class);

   private String[] includedClasses = null;
   private String[] excludedClasses = null;
   private String[] notFoundClasses = null;

   private Map<String, Class> classes = new java.util.HashMap<String, Class>();

   /**
    * Creates a new classloader that loads the given classes.
    * 
    * @param includedClasses
    *           array of class or package names that should be directly loaded by this loader.
    *           Classes whose name starts with any of the strings in this array will be loaded by
    *           this class, unless their name appears in <code>excludedClasses</code>. Can be
    *           <code>null</code>
    * @param excludedClasses
    *           array of class or package names that should NOT be directly loaded by this loader.
    *           Loading of classes whose name starts with any of the strings in this array will be
    *           delegated to <code>parent</code>, even if the classes package or classname appears
    *           in <code>includedClasses</code>. Typically this parameter is used to exclude loading
    *           one or more classes in a package whose other classes are loaded by this object.
    * @param parent
    *           ClassLoader to which loading of classes should be delegated if necessary
    */
   public SelectedClassnameClassLoader(String[] includedClasses, String[] excludedClasses, ClassLoader parent) {
      this(includedClasses, excludedClasses, null, parent);
   }

   /**
    * Creates a new classloader that loads the given classes.
    * 
    * @param includedClasses
    *           array of class or package names that should be directly loaded by this loader.
    *           Classes whose name starts with any of the strings in this array will be loaded by
    *           this class, unless their name appears in <code>excludedClasses</code>. Can be
    *           <code>null</code>
    * @param excludedClasses
    *           array of class or package names that should NOT be directly loaded by this loader.
    *           Loading of classes whose name starts with any of the strings in this array will be
    *           delegated to <code>parent</code>, even if the classes package or classname appears
    *           in <code>includedClasses</code>. Typically this parameter is used to exclude loading
    *           one or more classes in a package whose other classes are loaded by this object.
    * @param notFoundClasses
    *           array of class or package names for which this should raise a ClassNotFoundException
    * @param parent
    *           ClassLoader to which loading of classes should be delegated if necessary
    */
   public SelectedClassnameClassLoader(String[] includedClasses, String[] excludedClasses, String[] notFoundClasses,
            ClassLoader parent) {
      super(parent);
      this.includedClasses = includedClasses;
      this.excludedClasses = excludedClasses;
      this.notFoundClasses = notFoundClasses;

      log.debug("created " + this);
   }

   protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      log.trace("loadClass(" + name + "," + resolve + ")");
      if (isIncluded(name) && (isExcluded(name) == false)) {
         Class c = findClass(name);

         if (resolve) {
            resolveClass(c);
         }
         return c;
      } else if (isNotFound(name)) {
         throw new ClassNotFoundException(name + " is discarded");
      } else {
         return super.loadClass(name, resolve);
      }
   }

   protected Class<?> findClass(String name) throws ClassNotFoundException {
      log.trace("findClass(" + name + ")");
      Class result = classes.get(name);
      if (result != null) {
         return result;
      }

      if (isIncluded(name) && (isExcluded(name) == false)) {
         result = createClass(name);
      } else if (isNotFound(name)) {
         throw new ClassNotFoundException(name + " is discarded");
      } else {
         result = super.findClass(name);
      }

      classes.put(name, result);

      return result;
   }

   protected Class createClass(String name) throws ClassFormatError, ClassNotFoundException {
      log.info("createClass(" + name + ")");
      try {
         InputStream is = getResourceAsStream(name.replace('.', '/').concat(".class"));
         byte[] bytes = new byte[1024];
         ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
         int read;
         while ((read = is.read(bytes)) > -1) {
            baos.write(bytes, 0, read);
         }
         bytes = baos.toByteArray();
         return this.defineClass(name, bytes, 0, bytes.length);
      } catch (FileNotFoundException e) {
         throw new ClassNotFoundException("cannot find " + name, e);
      } catch (IOException e) {
         throw new ClassNotFoundException("cannot read " + name, e);
      }
   }

   protected boolean isIncluded(String className) {

      if (includedClasses != null) {
         for (int i = 0; i < includedClasses.length; i++) {
            if (className.startsWith(includedClasses[i])) {
               return true;
            }
         }
      }

      return false;
   }

   protected boolean isExcluded(String className) {

      if (excludedClasses != null) {
         for (int i = 0; i < excludedClasses.length; i++) {
            if (className.startsWith(excludedClasses[i])) {
               return true;
            }
         }
      }

      return false;
   }

   protected boolean isNotFound(String className) {

      if (notFoundClasses != null) {
         for (int i = 0; i < notFoundClasses.length; i++) {
            if (className.startsWith(notFoundClasses[i])) {
               return true;
            }
         }
      }

      return false;
   }

   public String toString() {
      String s = getClass().getName();
      s += "[includedClasses=";
      s += listClasses(includedClasses);
      s += ";excludedClasses=";
      s += listClasses(excludedClasses);
      s += ";notFoundClasses=";
      s += listClasses(notFoundClasses);
      s += ";parent=";
      s += getParent();
      s += "]";
      return s;
   }

   private static String listClasses(String[] classes) {
      if (classes == null)
         return null;
      String s = "";
      for (int i = 0; i < classes.length; i++) {
         if (i > 0)
            s += ",";
         s += classes[i];
      }
      return s;
   }

}
