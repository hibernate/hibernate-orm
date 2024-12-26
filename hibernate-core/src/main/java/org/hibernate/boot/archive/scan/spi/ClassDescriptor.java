/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
	String name();

	Categorization categorization();

	/**
	 * Retrieves access to the InputStream for the class file.
	 *
	 * @return Access to the InputStream for the class file.
	 */
	InputStreamAccess streamAccess();
}
