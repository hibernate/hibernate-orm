/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

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
public interface MetadataBuildingContext {
	/**
	 * Access to the options specified by the {@link org.hibernate.boot.MetadataBuilder}
	 *
	 * @return The options
	 */
	public MetadataBuildingOptions getBuildingOptions();

	/**
	 * Access to mapping defaults in effect for this context
	 *
	 * @return The mapping defaults.
	 */
	public MappingDefaults getMappingDefaults();

	/**
	 * Access to the collector of metadata as we build it.
	 *
	 * @return The metadata collector.
	 */
	public InFlightMetadataCollector getMetadataCollector();

	/**
	 * Provides access to ClassLoader services when needed during binding
	 *
	 * @return The ClassLoaderAccess
	 */
	public ClassLoaderAccess getClassLoaderAccess();

	/**
	 * Not sure how I feel about this exposed here
	 *
	 * @return
	 */
	public ObjectNameNormalizer getObjectNameNormalizer();
}
