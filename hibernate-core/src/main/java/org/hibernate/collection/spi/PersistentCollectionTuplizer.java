/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;

/**
 * PersistentCollection wraps Java collection types adding capabilities Hibernate
 * needs to load/persist them - this tuplizer defines extendable support for these
 * PersistentCollection.
 *
 * @author Steve Ebersole
 */
public interface PersistentCollectionTuplizer<T extends PersistentCollection> {
	/**
	 * Create a new instance of the wrapped bare/raw/naked collection
	 */
	Object instantiate(int anticipatedSize);

	/**
	 * Wrap the bare/raw/naked collection instance in a PersistentCollection wrapper.
	 * <p/>
	 * NOTE : Callers <b>MUST</b> add the holder to the persistence context!
	 */
	T wrap(SharedSessionContractImplementor session, Object rawCollection);

	<E> Class<E> getCollectionJavaType();

	Class<PersistentCollection> getPersistentCollectionJavaType();

	default Object indexOf(Object collection, Object element) {
		throw new UnsupportedOperationException( "generic collections don't have indexes" );
	}

	boolean contains(Object collection, Object childObject);

	<O, C, E> PluralPersistentAttribute<O, C, E> generatePluralPersistentAttribute(
			ManagedTypeDescriptor container,
			Property property,
			RuntimeModelCreationContext context);
}
