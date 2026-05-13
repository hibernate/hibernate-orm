/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

import static org.hibernate.collection.spi.InitializerProducerBuilder.createSetInitializerProducer;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSetSemantics<SE extends Set<E>,E> implements CollectionSemantics<SE,E> {
	@Override
	public Class<? extends Set> getCollectionJavaType() {
		return Set.class;
	}

	@Override
	public Iterator<E> getElementIterator(SE rawCollection) {
		return rawCollection == null ? null : rawCollection.iterator();
	}

	@Override
	public void visitElements(SE rawCollection, Consumer<? super E> action) {
		if ( rawCollection != null ) {
			rawCollection.forEach( action );
		}
	}

	@Override
	public int collectionSize(Object rawCollection) {
		return rawCollection instanceof Set<?> set ? set.size() : 0;
	}

	@Override
	public Object copy(
			Object rawCollection,
			CollectionPersister collectionDescriptor) {
		return rawCollection instanceof Set<?> set
				? instantiateWithElements( collectionSize( rawCollection ), collectionDescriptor, set )
				: rawCollection;
	}

	@Override
	public Set<?> copyPart(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			CollectionPart.Nature partNature) {
		final LinkedHashSet<Object> copy = new LinkedHashSet<>();
		if ( partNature == CollectionPart.Nature.INDEX ) {
			final int listIndexBase =
					collectionDescriptor.getAttributeMapping()
							.getIndexMetadata().getListIndexBase();
			for ( int i = 0; i < collectionSize( rawCollection ); i++ ) {
				copy.add( listIndexBase + i );
			}
		}
		else if ( rawCollection instanceof Set<?> set ) {
			copy.addAll( set );
		}
		return copy;
	}

	@Override
	public  CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		assert indexFetch == null;
		return createSetInitializerProducer(
				navigablePath,
				attributeMapping,
				fetchParent,
				selected,
				elementFetch,
				creationState
		);
	}
}
