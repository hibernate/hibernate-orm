/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;

/**
 * Models the requested fetching of a persistent collection attribute.
 *
 * @author Steve Ebersole
 */
public interface CollectionFetch extends Fetch, CollectionMappingNode {
	PluralPersistentAttribute getFetchedNavigable();

	@Override
	default NavigablePath getNavigablePath() {
		return getFetchParent().getNavigablePath().append( getFetchedNavigable().getNavigableName() );
	}
}
