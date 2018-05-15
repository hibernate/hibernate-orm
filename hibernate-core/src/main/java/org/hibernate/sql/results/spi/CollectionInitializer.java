/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;

/**
 * Initializer implementation for initializing collections (plural attributes)
 *
 * @author Steve Ebersole
 */
public interface CollectionInitializer extends Initializer {
	PluralPersistentAttribute getFetchedAttribute();

	NavigablePath getNavigablePath();

	PersistentCollection getCollectionInstance();

	@Override
	default Object getInitializedInstance() {
		return getCollectionInstance();
	}
}
