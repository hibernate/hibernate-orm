/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;

/**
 * Describes the context in which the process of building Metadata out of MetadataSources occurs.
 *
 * BindingContext are generally hierarchical getting more specific as we "go
 * down".  E.g.  global -> PU -> document -> mapping
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataBuildingContext extends ResolutionContext {
	@Override
	default MetadataBuildingContext getMetadataBuildingContext() {
		return this;
	}

	@Override
	BootstrapContext getBootstrapContext();

	TypeDefinition resolveTypeDefinition(String typeName);
	void addTypeDefinition(TypeDefinition typeDefinition);

	/**
	 * Access to the options specified by the {@link org.hibernate.boot.MetadataBuilder}
	 *
	 * @return The options
	 */
	MetadataBuildingOptions getBuildingOptions();

	/**
	 * Access to mapping defaults in effect for this context
	 *
	 * @return The mapping defaults.
	 */
	MappingDefaults getMappingDefaults();

	/**
	 * Access to the collector of metadata as we build it.
	 *
	 * @return The metadata collector.
	 */
	InFlightMetadataCollector getMetadataCollector();

	/**
	 * Not sure how I feel about this exposed here
	 *
	 * @return
	 */
	ObjectNameNormalizer getObjectNameNormalizer();

	/**
	 * Handles the more "global" resolution of this question.  Added mainly
	 * to cache this part of the resolution.  From the perspective of
	 * using this value JdbcRecommendedSqlTypeMappingContext#getPreferredSqlTypeCodeForBoolean
	 * is the one used for the resolution; this method simply acts as a fallback.
	 */
	int getPreferredSqlTypeCodeForBoolean();
}
