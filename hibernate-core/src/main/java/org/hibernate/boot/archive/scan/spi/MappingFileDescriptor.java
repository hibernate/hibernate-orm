/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.spi;

import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * Descriptor for a mapping (XML) file.
 *
 * @author Steve Ebersole
 */
public interface MappingFileDescriptor {
	/**
	 * The mapping file name.  This is its name within the archive, the
	 * expectation being that most times this will equate to a "classpath
	 * lookup resource name".
	 *
	 * @return The mapping file resource name.
	 */
	String getName();

	/**
	 * Retrieves access to the InputStream for the mapping file.
	 *
	 * @return Access to the InputStream for the mapping file.
	 */
	InputStreamAccess getStreamAccess();
}
