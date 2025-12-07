/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public class SqmCorrelatedDerivedRoot<T> extends SqmCorrelatedRoot<T> implements SqmPathWrapper<T, T>, SqmCorrelation<T, T> {

	public SqmCorrelatedDerivedRoot(SqmDerivedRoot<T> correlationParent) {
		this( (SqmRoot<T>) correlationParent );
	}

	public SqmCorrelatedDerivedRoot(SqmCteRoot<T> correlationParent) {
		this( (SqmRoot<T>) correlationParent );
	}

	private SqmCorrelatedDerivedRoot(SqmRoot<T> correlationParent) {
		super(
				correlationParent.getNavigablePath(),
				correlationParent.getReferencedPathSource(),
				correlationParent.nodeBuilder(),
				correlationParent
		);
	}

	@Override
	public SqmCorrelatedDerivedRoot<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmCorrelatedDerivedRoot<>( getCorrelationParent().copy( context ) )
		);
		copyTo( path, context );
		return path;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmEntityDomainType<T> getModel() {
		throw new UnsupportedOperationException( "Correlated derived root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Override
	public String getEntityName() {
		throw new UnsupportedOperationException( "Correlated derived root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Override
	public SqmPathSource<T> getResolvedModel() {
		return getReferencedPathSource();
	}

}
