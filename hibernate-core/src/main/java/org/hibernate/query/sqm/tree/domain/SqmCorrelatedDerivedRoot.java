/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
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
		final SqmCorrelatedDerivedRoot<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedDerivedRoot<T> path = context.registerCopy(
				this,
				new SqmCorrelatedDerivedRoot<>( getCorrelationParent().copy( context ) )
		);
		copyTo( path, context );
		return path;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public EntityDomainType<T> getModel() {
		// Or should we throw an exception instead?
		return null;
	}

	@Override
	public String getEntityName() {
		return null;
	}

	@Override
	public SqmPathSource<T> getResolvedModel() {
		return getReferencedPathSource();
	}

}
