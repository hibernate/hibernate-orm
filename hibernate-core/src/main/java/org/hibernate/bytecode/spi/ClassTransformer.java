/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.bytecode.spi;

import java.security.ProtectionDomain;

/**
 * A persistence provider provides an instance of this interface
 * to the PersistenceUnitInfo.addTransformer method.
 * The supplied transformer instance will get called to transform
 * entity class files when they are loaded and redefined.  The transformation
 * occurs before the class is defined by the JVM
 *
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Emmanuel Bernard
 */
public interface ClassTransformer
{
   /**
	* Invoked when a class is being loaded or redefined to add hooks for persistence bytecode manipulation
	*
	* @param loader the defining class loaderof the class being transformed.  It may be null if using bootstrap loader
	* @param classname The name of the class being transformed
	* @param classBeingRedefined If an already loaded class is being redefined, then pass this as a parameter
	* @param protectionDomain ProtectionDomain of the class being (re)-defined
	* @param classfileBuffer The input byte buffer in class file format
	* @return A well-formed class file that can be loaded
	*/
   public byte[] transform(ClassLoader loader,
					String classname,
					Class classBeingRedefined,
					ProtectionDomain protectionDomain,
					byte[] classfileBuffer);
}
