/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.spi;

/**
 * Contract for visiting an archive, which might be a jar, a zip, an exploded directory, etc.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public interface ArchiveDescriptor {
	/**
	 * Perform visitation using the given context
	 *
	 * @param archiveContext The visitation context
	 */
	public void visitArchive(ArchiveContext archiveContext);
}
