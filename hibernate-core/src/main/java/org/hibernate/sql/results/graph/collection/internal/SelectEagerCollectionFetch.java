/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Andrea Boriero
 */
public class SelectEagerCollectionFetch extends CollectionFetch {
	private final DomainResult<?> collectionKeyDomainResult;

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

	@Override
	public DomainResultAssembler<?> createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return new SelectEagerCollectionAssembler(
				getNavigablePath(),
				getFetchedMapping(),
				parentAccess,
				collectionKeyDomainResult == null ? null : collectionKeyDomainResult.createResultAssembler( null, creationState ),
				creationState
		);
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getFetchedMapping().getJavaType();
	}
}
