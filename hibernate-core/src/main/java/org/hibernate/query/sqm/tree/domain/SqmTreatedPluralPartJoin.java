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
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedPluralPartJoin<O,T, S extends T> extends SqmPluralPartJoin<O,S> implements SqmTreatedJoin<O,T,S> {
	private final SqmPluralPartJoin<O,T> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	public SqmTreatedPluralPartJoin(
			SqmPluralPartJoin<O,T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				(SqmFrom<?, O>) wrappedPath.getLhs(),
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName(),
						alias
				),
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedPluralPartJoin(
			NavigablePath navigablePath,
			SqmPluralPartJoin<O,T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				(SqmFrom<?, O>) wrappedPath.getLhs(),
				navigablePath,
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedPluralPartJoin<O, T, S> copy(SqmCopyContext context) {
		final SqmTreatedPluralPartJoin<O, T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedPluralPartJoin<O, T, S> path = context.registerCopy(
				this,
				new SqmTreatedPluralPartJoin<>(
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
	public SqmPluralPartJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public <S1 extends S> SqmTreatedPluralPartJoin<O, S, S1> treatAs(Class<S1> treatJavaType) {
		return super.treatAs( treatJavaType );
	}

	@Override
	public <S1 extends S> SqmTreatedPluralPartJoin<O, S, S1> treatAs(EntityDomainType<S1> treatTarget) {
		return (SqmTreatedPluralPartJoin<O, S, S1>) super.treatAs( treatTarget );
	}

	@Override
	public <S1 extends S> SqmTreatedPluralPartJoin<O, S, S1> treatAs(Class<S1> treatJavaType, String alias) {
		return (SqmTreatedPluralPartJoin<O, S, S1>) super.treatAs( treatJavaType, alias );
	}

	@Override
	public <S1 extends S> SqmTreatedPluralPartJoin<O, S, S1> treatAs(EntityDomainType<S1> treatTarget, String alias) {
		return (SqmTreatedPluralPartJoin<O, S, S1>) super.treatAs( treatTarget, alias );
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
