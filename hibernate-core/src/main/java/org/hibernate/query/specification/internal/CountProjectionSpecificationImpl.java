/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityHandler;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.SharedSessionContract;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.QuerySpecification;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.query.specification.SimpleProjectionSpecification;
import org.hibernate.query.spi.JpaTypedQueryReference;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * @author Gavin King
 */
public class CountProjectionSpecificationImpl<T> implements SimpleProjectionSpecification<T,Long>, JpaTypedQueryReference<Long> {

	private final SelectionSpecification<T> selectionSpecification;

	public CountProjectionSpecificationImpl(@Nonnull SelectionSpecification<T> specification) {
		this.selectionSpecification = specification;
	}

	@Nonnull
	@Override
	public QuerySpecification<T> restrict(@Nonnull Restriction<? super T> restriction) {
		throw new UnsupportedOperationException( "This is not supported yet!" );
	}

	@Nonnull
	@Override
	public SelectionQuery<Long> createQuery(@Nonnull EntityHandler entityHandler) {
		return entityHandler.unwrap( SharedSessionContract.class )
				.createQuery( buildCriteria( entityHandler.getCriteriaBuilder() ) );
	}

	@Nonnull
	@Override
	public CriteriaQuery<Long> buildCriteria(@Nonnull CriteriaBuilder builder) {
		var impl = (SelectionSpecificationImpl<T>) selectionSpecification;
		// TODO: handle HQL, existing criteria
		final var tupleQuery =
				(SqmSelectStatement<Long>)
						builder.createQuery( getResultType() );
		final var root = tupleQuery.from( impl.getResultType() );
		// This cast is completely bogus
		final var castStatement = (SqmSelectStatement<T>) tupleQuery;
		impl.getSpecifications().forEach( spec -> spec.accept( castStatement, root ) );
		tupleQuery.select( builder.count( root ) );
		return tupleQuery;
	}

	@Nonnull
	@Override
	public SimpleProjectionSpecification<T,Long> validate(@Nonnull CriteriaBuilder builder) {
		selectionSpecification.validate( builder );
		return this;
	}

	@Nonnull
	@Override
	public TypedQueryReference<Long> reference() {
		return this;
	}

	@Override
	@Nullable
	public String getName() {
		return null;
	}

	@Override
	@Nonnull
	public Class<Long> getResultType() {
		return Long.class;
	}

	@Override
	@Nullable
	public String getEntityGraphName() {
		return null;
	}

	@Override
	@Nonnull
	public Map<String, Object> getHints() {
		return emptyMap();
	}

	@Override
	@Nonnull
	public List<Class<?>> getParameterTypes() {
		return List.of();
	}

	@Override
	@Nonnull
	public List<String> getParameterNames() {
		return List.of();
	}

	@Override
	@Nonnull
	public List<Object> getArguments() {
		return List.of();
	}

	@Override
	@Nullable
	public Timeout getTimeout() {
		return null;
	}
}
