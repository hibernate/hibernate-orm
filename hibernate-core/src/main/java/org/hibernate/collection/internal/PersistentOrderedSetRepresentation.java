/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollectionRepresentation;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class PersistentOrderedSetRepresentation implements PersistentCollectionRepresentation {
	/**
	 * Singleton access
	 */
	public static final PersistentOrderedSetRepresentation INSTANCE = new PersistentOrderedSetRepresentation();

	private PersistentOrderedSetRepresentation() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ORDERED_SET;
	}

	@Override
	public Class getPersistentCollectionJavaType() {
		// for now we re-use the Set wrapper - the important distinction
		//		here is in the raw Set used (LinkedHashSet rather than HashSet)
		//		which is handled in the descriptor
		return PersistentSet.class;
	}

	@Override
	public PersistentCollectionDescriptor generatePersistentCollectionDescriptor(
			ManagedTypeDescriptor runtimeContainer,
			ManagedTypeMapping bootContainer,
			Property bootProperty,
			RuntimeModelCreationContext context) {
		throw new NotYetImplementedFor6Exception();
	}
}
