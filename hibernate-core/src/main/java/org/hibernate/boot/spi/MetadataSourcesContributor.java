/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.MetadataSources;

/**
 * A bootstrap process hook for contributing sources to MetadataSources.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataSourcesContributor {
	/**
	 * Perform the process of contributing to MetadataSources.
	 *
	 * @param metadataSources The MetadataSources, to which to contribute.
	 */
	public void contribute(MetadataSources metadataSources);
}
