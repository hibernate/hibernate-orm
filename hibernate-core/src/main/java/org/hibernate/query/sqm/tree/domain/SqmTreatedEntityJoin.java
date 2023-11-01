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
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedEntityJoin<L,R,S extends R> extends SqmEntityJoin<L,S> implements SqmTreatedJoin<L,R,S> {
	private final SqmEntityJoin<L,R> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	public SqmTreatedEntityJoin(
			SqmEntityJoin<L,R> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias) {
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName(),
						alias
				),
				treatTarget,
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	private SqmTreatedEntityJoin(
			NavigablePath navigablePath,
			SqmEntityJoin<L,R> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias) {
		super(
				navigablePath,
				treatTarget,
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmTreatedEntityJoin<L,R,S> copy(SqmCopyContext context) {
		final SqmTreatedEntityJoin<L,R,S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedEntityJoin<L,R,S> path = context.registerCopy(
				this,
				new SqmTreatedEntityJoin<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget,
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<S> getModel() {
		return getTreatTarget();
	}

	@Override
	public SqmPath<R> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		wrappedPath.appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getName() );
		sb.append( ')' );
	}
}
