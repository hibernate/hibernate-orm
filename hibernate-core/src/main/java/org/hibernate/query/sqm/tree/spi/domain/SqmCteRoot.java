/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.spi.NavigablePath;

import java.util.Objects;

/**
 * @author Christian Beikov
 */
@Incubating
public class SqmCteRoot<T> extends SqmRoot<T> implements JpaRoot<T> {

	private final SqmCteStatement<T> cte;

	public SqmCteRoot(
			SqmCteStatement<T> cte,
			@Nullable String alias) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<cte>>", alias ),
				cte,
				(SqmPathSource<T>) cte.getCteTable().getTupleType(),
				alias
		);
	}

	protected SqmCteRoot(
			NavigablePath navigablePath,
			SqmCteStatement<T> cte,
			SqmPathSource<T> pathSource,
			@Nullable String alias) {
		super(
				navigablePath,
				pathSource,
				alias,
				true,
				cte.nodeBuilder()
		);
		this.cte = cte;
	}

	@Override
	public SqmCteRoot<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmCteRoot<>(
						getNavigablePath(),
						getCte().copy( context ),
						getReferencedPathSource(),
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	public SqmCteStatement<T> getCte() {
		return cte;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRootCte( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Nonnull
	@Override
	public SqmEntityDomainType<T> getModel() {
		throw new UnsupportedOperationException( "Cte root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Override
	public String getEntityName() {
		throw new UnsupportedOperationException( "Cte root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Override
	public SqmPathSource<T> getResolvedModel() {
		return getReferencedPathSource();
	}

	@Override
	@Nonnull
	public SqmCorrelatedRoot<T> createCorrelation() {
		return new SqmCorrelatedDerivedRoot<>( this );
	}

	@Override
	public boolean deepEquals(SqmFrom<?, ?> object) {
		return super.deepEquals( object )
			&& Objects.equals( cte.getCteTable().getCteName(), ((SqmCteRoot<?>) object).cte.getCteTable().getCteName() );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& Objects.equals( cte.getCteTable().getCteName(), ((SqmCteRoot<?>) object).cte.getCteTable().getCteName() );
	}
}
