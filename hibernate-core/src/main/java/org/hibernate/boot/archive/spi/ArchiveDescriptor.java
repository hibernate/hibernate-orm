/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.spi;

/**
 * Models a logical archive, which might be <ul>
 *     <li>a jar file</li>
 *     <li>a zip file</li>
 *     <li>an exploded directory</li>
 *     <li>etc</li>
 * </ul>
 * <p>
 * Used mainly for scanning purposes via {@linkplain #visitArchive visitation}
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
	void visitArchive(ArchiveContext archiveContext);
}
