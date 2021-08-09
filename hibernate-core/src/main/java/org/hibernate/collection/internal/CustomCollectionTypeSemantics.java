/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.BagInitializerProducer;
import org.hibernate.sql.results.graph.collection.internal.ListInitializerProducer;
import org.hibernate.sql.results.graph.collection.internal.MapInitializerProducer;
import org.hibernate.type.CollectionType;

/**
 * A collection semantics wrapper for <code>CollectionType</code>.
 *
 * @author Christian Beikov
 */
public class CustomCollectionTypeSemantics<CE, E> implements CollectionSemantics<CE, E> {

	private final CollectionType collectionType;

	public CustomCollectionTypeSemantics(CollectionType collectionType) {
		this.collectionType = collectionType;
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.BAG;
	}

	@Override
	public Class<?> getCollectionJavaType() {
		return collectionType.getReturnedClass();
	}

	@Override
	public CE instantiateRaw(int anticipatedSize, CollectionPersister collectionDescriptor) {
		return (CE) collectionType.instantiate( anticipatedSize );
	}

	@Override
	public Iterator<E> getElementIterator(CE rawCollection) {
		return collectionType.getElementsIterator( rawCollection, null );
	}

	@Override
	public void visitElements(CE rawCollection, Consumer<? super E> action) {
		getElementIterator( rawCollection ).forEachRemaining( action );
	}

	@Override
	public CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final Fetch indexFetch;
		if ( attributeMapping.getIndexDescriptor() == null ) {
			indexFetch = null;
		}
		else {
			indexFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getIndexDescriptor(),
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}
		final Fetch elementFetch = fetchParent.generateFetchableFetch(
				attributeMapping.getElementDescriptor(),
				navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
				FetchTiming.IMMEDIATE,
				selected,
				null,
				creationState
		);
		if ( indexFetch == null ) {
			return new BagInitializerProducer( attributeMapping, null, elementFetch );
		}
		else if ( indexFetch.getResultJavaTypeDescriptor().getJavaTypeClass() == Integer.class ) {
			return new ListInitializerProducer( attributeMapping, indexFetch, elementFetch );
		}
		else {
			return new MapInitializerProducer( attributeMapping, indexFetch, elementFetch );
		}
	}

	@Override
	public CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		if ( indexFetch == null ) {
			indexFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getIndexDescriptor(),
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}
		if ( elementFetch == null ) {
			elementFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getElementDescriptor(),
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}
		if ( indexFetch == null ) {
			return new BagInitializerProducer( attributeMapping, null, elementFetch );
		}
		else if ( indexFetch.getResultJavaTypeDescriptor().getJavaTypeClass() == Integer.class ) {
			return new ListInitializerProducer( attributeMapping, indexFetch, elementFetch );
		}
		else {
			return new MapInitializerProducer( attributeMapping, indexFetch, elementFetch );
		}
	}

	@Override
	public PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return collectionType.instantiate( session, collectionDescriptor, key );
	}

	@Override
	public PersistentCollection<E> wrap(
			CE rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return collectionType.wrap( session, rawCollection );
	}
}
