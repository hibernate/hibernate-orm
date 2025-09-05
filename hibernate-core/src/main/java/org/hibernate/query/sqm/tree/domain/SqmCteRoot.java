/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
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
			String alias) {
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
			String alias) {
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
		final SqmCteRoot<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCteRoot<T> path = context.registerCopy(
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

	@Override
	public SqmEntityDomainType<T> getModel() {
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

	@Override
	public SqmCorrelatedRoot<T> createCorrelation() {
		return new SqmCorrelatedDerivedRoot<>( this );
	}

	@Override
	public boolean equals(Object object) {
		return super.equals( object )
			&& object instanceof SqmCteRoot<?> that
			&& Objects.equals( this.cte.getCteTable().getCteName(), that.cte.getCteTable().getCteName() );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + cte.getCteTable().getCteName().hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return super.isCompatible( object )
			&& object instanceof SqmCteRoot<?> that
			&& Objects.equals( this.cte.getCteTable().getCteName(), that.cte.getCteTable().getCteName() );
	}

	@Override
	public int cacheHashCode() {
		int result = super.cacheHashCode();
		result = 31 * result + cte.getCteTable().getCteName().hashCode();
		return result;
	}
}
