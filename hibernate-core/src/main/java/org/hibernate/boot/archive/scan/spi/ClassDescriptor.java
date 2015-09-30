/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.spi;

import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * Descriptor for a class file.
 *
 * @author Steve Ebersole
 */
public interface ClassDescriptor {
	enum Categorization {
		MODEL,
		CONVERTER,
		OTHER
	}

	/**
	 * Retrieves the class name, not the file name.
	 *
	 * @return The name (FQN) of the class
	 */
	String getName();

	Categorization getCategorization();

	/**
	 * Retrieves access to the InputStream for the class file.
	 *
	 * @return Access to the InputStream for the class file.
	 */
	InputStreamAccess getStreamAccess();
}
