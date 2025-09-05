/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;


/**
 * @author Christian Beikov
 */
@Incubating
public class SqmDerivedRoot<T> extends SqmRoot<T> implements JpaDerivedRoot<T> {

	private final SqmSubQuery<T> subQuery;

	public SqmDerivedRoot(
			SqmSubQuery<T> subQuery,
			String alias) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<derived>>", alias ),
				subQuery,
				new AnonymousTupleType<>( subQuery ),
				alias
		);
	}

	protected SqmDerivedRoot(
			NavigablePath navigablePath,
			SqmSubQuery<T> subQuery,
			SqmPathSource<T> pathSource,
			String alias) {
		super(
				navigablePath,
				pathSource,
				alias,
				true,
				subQuery.nodeBuilder()
		);
		this.subQuery = subQuery;
	}

	@Override
	public SqmDerivedRoot<T> copy(SqmCopyContext context) {
		final SqmDerivedRoot<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmDerivedRoot<T> path = context.registerCopy(
				this,
				new SqmDerivedRoot<>(
						getNavigablePath(),
						getQueryPart().copy( context ),
						getReferencedPathSource(),
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmSubQuery<T> getQueryPart() {
		return subQuery;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRootDerived( this );
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
	public <S extends T> SqmTreatedFrom<T, T, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Derived roots can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedFrom<T, T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new UnsupportedOperationException( "Derived roots can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedRoot treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Derived roots can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedRoot treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Derived roots can not be treated" );
	}

	@Override
	public boolean equals(Object object) {
		return super.equals( object )
			&& object instanceof SqmDerivedRoot<?> that
			&& subQuery.equals( that.subQuery );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + subQuery.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return super.isCompatible( object )
			&& object instanceof SqmDerivedRoot<?> that
			&& subQuery.isCompatible( that.subQuery );
	}

	@Override
	public int cacheHashCode() {
		int result = super.cacheHashCode();
		result = 31 * result + subQuery.cacheHashCode();
		return result;
	}
}
