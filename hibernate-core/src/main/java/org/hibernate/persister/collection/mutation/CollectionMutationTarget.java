/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.model.MutationTarget;

/**
 * @author Steve Ebersole
 */
public interface CollectionMutationTarget extends MutationTarget<CollectionTableMapping> {
	@Override
	PluralAttributeMapping getTargetPart();

	CollectionTableMapping getCollectionTableMapping();

	@Override
	default CollectionTableMapping getIdentifierTableMapping() {
		return getCollectionTableMapping();
	}

	/**
	 * Whether the collection has at least one physical index column
	 */
	boolean hasPhysicalIndexColumn();
}
