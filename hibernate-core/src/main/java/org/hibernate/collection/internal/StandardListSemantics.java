/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.collection.internal.ListInitializerProducer;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * Hibernate's standard CollectionSemantics for Lists
 *
 * @author Steve Ebersole
 */
public class StandardListSemantics<E> implements CollectionSemantics<List<E>, E> {
	/**
	 * Singleton access
	 */
	public static final StandardListSemantics<?> INSTANCE = new StandardListSemantics<>();

	private StandardListSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	public Class<List> getCollectionJavaType() {
		return List.class;
	}

	@Override
	public List<E> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return CollectionHelper.arrayList( anticipatedSize );
	}

	@Override
	public Iterator<E> getElementIterator(List<E> rawCollection) {
		return rawCollection.iterator();
	}

	@Override
	public void visitElements(List<E> rawCollection, Consumer<? super E> action) {
		rawCollection.forEach( action );
	}

	@Override
	public CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState) {
		return new ListInitializerProducer(
				attributeMapping,
				attributeMapping.getIndexDescriptor().generateFetch(
						fetchParent,
						navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
						FetchTiming.IMMEDIATE,
						selected,
						lockMode,
						null,
						creationState
				),
				attributeMapping.getElementDescriptor().generateFetch(
						fetchParent,
						navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
						FetchTiming.IMMEDIATE,
						selected,
						lockMode,
						null,
						creationState
				)
		);
	}

	@Override
	public CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		if ( indexFetch == null ) {
			indexFetch = attributeMapping.getIndexDescriptor().generateFetch(
					fetchParent,
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					lockMode,
					null,
					creationState
			);
		}
		if ( elementFetch == null ) {
			elementFetch = attributeMapping.getElementDescriptor().generateFetch(
					fetchParent,
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					lockMode,
					null,
					creationState
			);
		}
		return new ListInitializerProducer(
				attributeMapping,
				indexFetch,
				elementFetch
		);
	}

	@Override
	public PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentList<>( session );
	}

	@Override
	public PersistentCollection<E> wrap(
			List<E> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentList<>( session, rawCollection );
	}
}
