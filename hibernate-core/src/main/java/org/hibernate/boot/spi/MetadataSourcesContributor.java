/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.MetadataSources;
import org.hibernate.service.JavaServiceLoadable;

/**
 * A bootstrap process hook for contributing sources to {@link MetadataSources}.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
@JavaServiceLoadable
public interface MetadataSourcesContributor {
	/**
	 * Perform the process of contributing to the {@link MetadataSources}.
	 *
	 * @param metadataSources The {@code MetadataSource}s, to which to contribute.
	 */
	void contribute(MetadataSources metadataSources);
}
