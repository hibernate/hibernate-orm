/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.results.spi;

import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.CollectionAttributeFetch;
import org.hibernate.type.CollectionType;

/**
 * Models the requested fetching of a persistent collection attribute.
 *
 * @author Steve Ebersole
 */
public interface FetchCollectionAttribute extends FetchAttribute, CollectionReference, CollectionAttributeFetch {
	/**
	 * Get the Hibernate Type that describes the fetched attribute as a {@link CollectionType}.
	 *
	 * @return The Type of the fetched attribute
	 */
	@Override
	CollectionType getFetchedType();

	@Override
	default PropertyPath getNavigablePath() {
		return getSource().getPropertyPath().append( getFetchedAttributeDescriptor().getAttributeName() );
	}
}
