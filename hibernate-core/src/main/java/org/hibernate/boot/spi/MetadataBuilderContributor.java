/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributor;

/**
 * A bootstrap process hook for contributing settings to {@link MetadataBuilder}.
 * <p/>
 * Generally this is used from JPA bootstrapping where {@link MetadataBuilder} is not accessible.
 * <p/>
 * Implementations can be {@linkplain java.util.ServiceLoader discovered}. For historical reasons,
 * an implementation can also be named using the
 * {@value org.hibernate.jpa.boot.spi.JpaSettings#METADATA_BUILDER_CONTRIBUTOR} setting, though
 * discovery should be preferred.
 *
 * @author Vlad Mihalcea
 *
 * @since 5.3
 *
 * @deprecated Use settings, {@link TypeContributor}, {@link FunctionContributor} or
 * {@link AdditionalMappingContributor} instead depending on need
 */
@Deprecated(forRemoval = true)
public interface MetadataBuilderContributor {
	/**
	 * Perform the process of contributing to the {@link MetadataBuilder}.
	 *
	 * @param metadataBuilder The {@link MetadataBuilder}, to which to contribute.
	 */
	void contribute(MetadataBuilder metadataBuilder);
}
