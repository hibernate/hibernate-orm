/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.spi;

/**
 * Represent an entry in the archive.
 *
 * @author Steve Ebersole
 */
public interface ArchiveEntry {
	/**
	 * Get the entry's name
	 *
	 * @return The name
	 */
	public String getName();

	/**
	 * Get the relative name of the entry within the archive.  Typically what we are looking for here is
	 * the ClassLoader resource lookup name.
	 *
	 * @return The name relative to the archive root
	 */
	public String getNameWithinArchive();

	/**
	 * Get access to the stream for the entry
	 *
	 * @return Obtain stream access to the entry
	 */
	public InputStreamAccess getStreamAccess();
}
