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
package org.hibernate.metamodel.archive.scan.spi;

import org.hibernate.metamodel.archive.spi.InputStreamAccess;

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
