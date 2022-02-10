/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain;

import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.Dialect;

/**
 * Describes a standard domain model
 *
 * @see StandardDomainModel
 * @see org.hibernate.testing.orm.junit.DomainModel
 * @see org.hibernate.testing.orm.junit.DomainModelFunctionalTesting
 * @see org.hibernate.testing.orm.junit.DomainModelExtension
 *
 * @author Steve Ebersole
 */
public interface DomainModelDescriptor {

	Class[] getAnnotatedClasses();

	/**
	 * Apply the model classes to the given MetadataSources
	 */
	void applyDomainModel(MetadataSources sources);

	/**
	 * The namespace to apply the model to.  This is interpreted as a catalog
	 * name or a schema name depending on the capability of the underlying database
	 * via {@link Dialect}.  Would require a new Dialect method I think, though
	 * we could also leverage the driver's db-metadata to ascertain which interpretation
	 * to use which would not need any (more) test-specific Dialect feature.
	 *
	 * Note however that this might be a useful feature as well for users instead of
	 * JPA's {@link javax.persistence.Table#catalog} / {@link javax.persistence.Table#schema}.
	 * AKA, something like `@org.hibernate.annotations.Namespace("a_name")` or
	 * `@org.hibernate.annotations.Table( namespace="a_name", ... )`.
	 *
	 * This may be {@code null} indicating that the default namespace should be used.
	 *
	 * Note that domain models can use the same namespace so long as they do not share
	 * db-object (tables, etc) names
	 */
	default String getNamespace() {
		return null;
	}

	/**
	 * Identifies the specific mapping features this domain model uses.
	 */
	default EnumSet<MappingFeature> getMappingFeaturesUsed() {
		// for now just return none.  this is simply informative, not used to
		// drive any functionality - so maybe it's not important to add
		return EnumSet.noneOf( MappingFeature.class );
	}
}
