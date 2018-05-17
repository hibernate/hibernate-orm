/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.MetadataBuilder;

/**
 * A bootstrap process hook for contributing settings to {@link MetadataBuilder}.
 *
 * @author Vlad Mihalcea
 *
 * @since 5.3
 */
public interface MetadataBuilderContributor {
	/**
	 * Perform the process of contributing to MetadataSources.
	 *
	 * @param metadataBuilder The {@link MetadataBuilder}, to which to contribute.
	 */
	void contribute(MetadataBuilder metadataBuilder);
}
