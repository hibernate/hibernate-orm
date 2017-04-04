/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.spi;

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
