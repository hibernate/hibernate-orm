/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.MapSemantics;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.collection.internal.MapInitializerProducer;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMapSemantics<M extends Map<?,?>> implements MapSemantics<M> {
	@Override
	public Class<M> getCollectionJavaType() {
		//noinspection unchecked
		return (Class) Map.class;
	}

	@Override
	public Iterator getKeyIterator(M rawMap) {
		if ( rawMap == null ) {
			return null;
		}

		return rawMap.keySet().iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitKeys(M rawMap, Consumer action) {
		if ( rawMap != null ) {
			rawMap.keySet().forEach( action );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitEntries(M rawMap, BiConsumer action) {
		if ( rawMap != null ) {
			rawMap.forEach( action );
		}
	}


	@Override
	public Iterator getElementIterator(Map rawMap) {
		if ( rawMap == null ) {
			return Collections.emptyIterator();
		}

		return rawMap.values().iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitElements(M rawMap, Consumer action) {
		if ( rawMap != null ) {
			rawMap.values().forEach( action );
		}
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
		return new MapInitializerProducer(
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
			DomainResultCreationState creationState){
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
		return new MapInitializerProducer(
				attributeMapping,
				indexFetch,
				elementFetch
		);
	}

}
