/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaFunctionRoot;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.NavigablePath;

import java.util.Objects;


/**
 * @author Christian Beikov
 */
@Incubating
public class SqmFunctionRoot<E> extends SqmRoot<E> implements JpaFunctionRoot<E> {

	private final SqmSetReturningFunction<E> function;

	public SqmFunctionRoot(SqmSetReturningFunction<E> function, String alias) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<derived>>", alias ),
				function,
				function.getType(),
				alias
		);
	}

	protected SqmFunctionRoot(
			NavigablePath navigablePath,
			SqmSetReturningFunction<E> function,
			SqmPathSource<E> pathSource,
			String alias) {
		super(
				navigablePath,
				pathSource,
				alias,
				true,
				function.nodeBuilder()
		);
		this.function = function;
	}

	@Override
	public SqmFunctionRoot<E> copy(SqmCopyContext context) {
		final SqmFunctionRoot<E> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFunctionRoot<E> path = context.registerCopy(
				this,
				new SqmFunctionRoot<>(
						getNavigablePath(),
						getFunction().copy( context ),
						getReferencedPathSource(),
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmSetReturningFunction<E> getFunction() {
		return function;
	}

	@Override
	public SqmPath<Long> index() {
		//noinspection unchecked
		final SqmPathSource<Long> indexPathSource = (SqmPathSource<Long>) function.getType().getSubPathSource( CollectionPart.Nature.INDEX.getName() );
		return resolvePath( indexPathSource.getPathName(), indexPathSource );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRootFunction( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmEntityDomainType<E> getModel() {
		// Or should we throw an exception instead?
		return null;
	}

	@Override
	public String getEntityName() {
		return null;
	}

	@Override
	public SqmPathSource<E> getResolvedModel() {
		return getReferencedPathSource();
	}

	@Override
	public SqmCorrelatedRoot<E> createCorrelation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends E> SqmTreatedFrom<E, E, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Function roots can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedFrom<E, E, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new UnsupportedOperationException( "Function roots can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedRoot treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Function roots can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedRoot treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Function roots can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedRoot treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		throw new UnsupportedOperationException( "Function roots can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedRoot treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		throw new UnsupportedOperationException( "Function roots can not be treated" );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmFunctionRoot<?> that
			&& super.equals( object )
			&& Objects.equals( this.function, that.function );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), function );
	}
}
