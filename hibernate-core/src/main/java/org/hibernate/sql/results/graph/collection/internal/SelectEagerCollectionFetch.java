/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.BitSet;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Andrea Boriero
 */
public class SelectEagerCollectionFetch extends CollectionFetch {
	private final @Nullable DomainResult<?> collectionKeyDomainResult;

	public SelectEagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			DomainResult<?> collectionKeyDomainResult,
			FetchParent fetchParent) {
		super( fetchedPath, fetchedAttribute, fetchParent );
		this.collectionKeyDomainResult = collectionKeyDomainResult;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.DELAYED;
	}

	@Override
	public boolean hasTableGroup() {
		return false;
	}

	public CollectionInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new SelectEagerCollectionInitializer(
				getNavigablePath(),
				getFetchedMapping(),
				parent,
				collectionKeyDomainResult,
				creationState
		);
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getFetchedMapping().getJavaType();
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		if ( collectionKeyDomainResult != null ) {
			collectionKeyDomainResult.collectValueIndexesToCache( valueIndexes );
		}
	}
}
