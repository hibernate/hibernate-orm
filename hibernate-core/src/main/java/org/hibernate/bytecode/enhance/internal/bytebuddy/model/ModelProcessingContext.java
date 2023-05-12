/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import org.hibernate.boot.spi.ClassLoaderAccess;

/**
 * @author Steve Ebersole
 */
public interface ModelProcessingContext {

	/**
	 * Registry of managed-classes
	 */
	ClassDetailsRegistry getClassDetailsRegistry();

	/**
	 * If model processing code needs to load things from the class-loader, they should
	 * really use this access.
	 */
	ClassLoaderAccess getClassLoaderAccess();
}
