/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmCorrelatedDerivedRootJoin<T> extends SqmCorrelatedRootJoin<T> {

	public SqmCorrelatedDerivedRootJoin(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedNavigable,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, nodeBuilder );
	}

	@Override
	public SqmCorrelatedDerivedRootJoin<T> copy(SqmCopyContext context) {
		final SqmCorrelatedDerivedRootJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedDerivedRootJoin<T> path = context.registerCopy(
				this,
				new SqmCorrelatedDerivedRootJoin<>(
						getNavigablePath(),
						getReferencedPathSource(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@SuppressWarnings("unchecked")
	public static <X, J extends SqmJoin<X, ?>> SqmCorrelatedDerivedRootJoin<X> create(J correlationParent, J correlatedJoin) {
		final SqmFrom<?, X> parentPath = (SqmFrom<?, X>) correlationParent.getParentPath();
		final SqmCorrelatedDerivedRootJoin<X> rootJoin;
		if ( parentPath == null ) {
			rootJoin = new SqmCorrelatedDerivedRootJoin<>(
					correlationParent.getNavigablePath(),
					(SqmPathSource<X>) correlationParent.getReferencedPathSource(),
					correlationParent.nodeBuilder()
			);
		}
		else {
			rootJoin = new SqmCorrelatedDerivedRootJoin<>(
					parentPath.getNavigablePath(),
					parentPath.getReferencedPathSource(),
					correlationParent.nodeBuilder()
			);
		}
		rootJoin.addSqmJoin( correlatedJoin );
		return rootJoin;
	}

	@Override
	public boolean containsOnlyInnerJoins() {
		// The derived join is just referenced, no need to create any table groups
		return true;
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
