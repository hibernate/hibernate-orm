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

/**
 * Options for performing scanning
 *
 * @author Steve Ebersole
 */
public interface ScanOptions {
	/**
	 * Is detection of managed classes from root url allowed?  In strict JPA
	 * sense, this would be controlled by the {@code <exclude-unlisted-classes/>}
	 * element.
	 *
	 * @return Whether detection of classes from root url is allowed
	 */
	public boolean canDetectUnlistedClassesInRoot();

	/**
	 * Is detection of managed classes from non-root urls allowed?  In strict JPA
	 * sense, this would always be allowed.
	 *
	 * @return Whether detection of classes from non-root urls is allowed
	 */
	public boolean canDetectUnlistedClassesInNonRoot();

	/**
	 * Is detection of Hibernate Mapping files allowed?
	 *
	 * @return Whether detection of Mapping files is allowed.
	 *
	 * @deprecated With move to unified schema, this setting is now deprecated and will
	 * be removed once support for reading {@code hbm.xml} files is fully removed.
	 */
	@Deprecated
	public boolean canDetectHibernateMappingFiles();
}
