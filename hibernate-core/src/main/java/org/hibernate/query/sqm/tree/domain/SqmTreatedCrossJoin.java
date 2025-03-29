/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.spi.NavigablePath;

/**
 * A TREAT form of {@linkplain SqmCrossJoin}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class SqmTreatedCrossJoin extends SqmCrossJoin implements SqmTreatedJoin {
	private final SqmCrossJoin wrappedPath;
	private final EntityDomainType treatTarget;

	private SqmTreatedCrossJoin(
			NavigablePath navigablePath,
			SqmCrossJoin<?> wrappedPath,
			EntityDomainType<?> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				navigablePath,
				(EntityDomainType) wrappedPath.getReferencedPathSource().getSqmPathType(),
				alias,
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmTreatedCrossJoin copy(SqmCopyContext context) {
		final SqmTreatedCrossJoin existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedCrossJoin path = context.registerCopy(
				this,
				new SqmTreatedCrossJoin(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget,
						getExplicitAlias()
				)
		);
		//noinspection unchecked
		copyTo( path, context );
		return path;
	}

	@Override
	public EntityDomainType getTreatTarget() {
		return treatTarget;
	}

	@Override
	public EntityDomainType getModel() {
		return getTreatTarget();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SqmPath getWrappedPath() {
		return wrappedPath;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public SqmPathSource getNodeType() {
		return treatTarget;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public EntityDomainType getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql );
		hql.append( " as " );
		hql.append( treatTarget.getName() );
		hql.append( ')' );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public SqmTreatedCrossJoin treatAs(Class treatJavaType, String alias) {
		return super.treatAs( treatJavaType, alias );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public SqmTreatedCrossJoin treatAs(EntityDomainType treatTarget, String alias) {
		return super.treatAs( treatTarget, alias );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public SqmTreatedCrossJoin treatAs(Class treatAsType) {
		return super.treatAs( treatAsType );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public SqmTreatedCrossJoin treatAs(EntityDomainType treatAsType) {
		return super.treatAs( treatAsType );
	}
}
