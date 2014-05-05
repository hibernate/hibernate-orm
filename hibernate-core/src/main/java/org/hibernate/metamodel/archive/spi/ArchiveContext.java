/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.archive.spi;

/**
 * Describes the context for visiting the entries within an archive
 *
* @author Steve Ebersole
*/
public interface ArchiveContext {
	/**
	 * Is the archive described (and being visited) the root url for the persistence-unit?
	 *
	 * @return {@code true} if it is the root url
	 */
	public boolean isRootUrl();

	/**
	 * Get the handler for the given entry, which generally is indicated by the entry type (a {@code .class} file, a
	 * mapping file, etc).
	 *
	 * @param entry The archive entry
	 *
	 * @return The appropriate handler for the entry
	 */
	public ArchiveEntryHandler obtainArchiveEntryHandler(ArchiveEntry entry);
}
