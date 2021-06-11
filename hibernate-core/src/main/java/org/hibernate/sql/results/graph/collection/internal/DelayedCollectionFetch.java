/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.UnfetchedResultAssembler;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionFetch extends CollectionFetch {

	private final DomainResult fkResult;

	public DelayedCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			FetchParent fetchParent,
			DomainResult fkResult) {
		super( fetchedPath, fetchedAttribute, fetchParent );
		this.fkResult = fkResult;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		// lazy attribute
		if ( fkResult == null ) {
			return new UnfetchedResultAssembler<>( getResultJavaTypeDescriptor() );
		}
		else {
			return new DelayedCollectionAssembler(
					getNavigablePath(),
					getFetchedMapping(),
					parentAccess,
					fkResult,
					creationState
			);
		}
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
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return getFetchedMapping().getJavaTypeDescriptor();
	}
}
