/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;

/**
 * Hibernate's standard PersistentCollectionDescriptor implementor
 * for Lists
 *
 * @author Steve Ebersole
 */
public class PersistentSetDescriptorImpl extends AbstractPersistentCollectionDescriptor {
	public PersistentSetDescriptorImpl(
			Property bootProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext context) {
		super( bootProperty, runtimeContainer, CollectionClassification.SET, context );
	}

	@Override
	protected CollectionJavaDescriptor resolveCollectionJtd(RuntimeModelCreationContext creationContext) {
		return findCollectionJtd( Set.class, creationContext );
	}

	@Override
	public Object instantiateRaw(int anticipatedSize) {
		return new HashMap<>(
				CollectionHelper.determineProperSizing( anticipatedSize )
		);
	}

	@Override
	public PersistentCollection instantiateWrapper(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Serializable key) {
		return new PersistentSet( session, descriptor, key );
	}

	@Override
	public boolean contains(Object collection, Object childObject) {
		return ( (Set) collection ).contains( childObject );
	}

	@Override
	public PersistentCollection wrap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Object rawCollection) {
		return new PersistentSet( session, descriptor, (Set) rawCollection );
	}

	@Override
	protected Table resolveCollectionTable(
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception();
	}
}
