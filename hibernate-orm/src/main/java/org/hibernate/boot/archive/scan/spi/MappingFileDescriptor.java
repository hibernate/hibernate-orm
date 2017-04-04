/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public String getName();

	/**
	 * Retrieves access to the InputStream for the mapping file.
	 *
	 * @return Access to the InputStream for the mapping file.
	 */
	public InputStreamAccess getStreamAccess();
}
