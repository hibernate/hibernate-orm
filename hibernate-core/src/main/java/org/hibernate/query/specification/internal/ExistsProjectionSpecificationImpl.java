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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 */
public class ExistsProjectionSpecificationImpl<T> implements SimpleProjectionSpecification<T,Boolean>, JpaTypedQueryReference<Boolean> {

	private final SelectionSpecification<T> selectionSpecification;

	public ExistsProjectionSpecificationImpl(@Nonnull SelectionSpecification<T> specification) {
		this.selectionSpecification = specification;
	}

	@Nonnull
	@Override
	public QuerySpecification<T> restrict(@Nonnull Restriction<? super T> restriction) {
		throw new UnsupportedOperationException( "This is not supported yet!" );
	}

	@Nonnull
	@Override
	public SelectionQuery<Boolean> createQuery(@Nonnull EntityHandler entityHandler) {
		return entityHandler.unwrap( SharedSessionContract.class )
				.createQuery( buildCriteria( entityHandler.getCriteriaBuilder() ) );
	}

	@Nonnull
	@Override
	public CriteriaQuery<Boolean> buildCriteria(@Nonnull CriteriaBuilder builder) {
		var impl = (SelectionSpecificationImpl<T>) selectionSpecification;
		// TODO: handle HQL, existing criteria
		final var tupleQuery =
				(SqmSelectStatement<Boolean>)
						builder.createQuery( getResultType() );
		final var subquery = tupleQuery.subquery( impl.getResultType() );
		final var root = subquery.from( impl.getResultType() );
		impl.getSpecifications().forEach( spec -> spec.accept( subquery, root ) );
		subquery.select( root );
		tupleQuery.select( builder.exists( subquery ) );
		return tupleQuery;
	}

	@Nonnull
	@Override
	public SimpleProjectionSpecification<T,Boolean> validate(@Nonnull CriteriaBuilder builder) {
		selectionSpecification.validate( builder );
		return this;
	}

	@Nonnull
	@Override
	public TypedQueryReference<Boolean> reference() {
		return this;
	}

	@Override
	@Nullable
	public String getName() {
		return null;
	}

	@Override
	@Nonnull
	public Class<Boolean> getResultType() {
		return Boolean.class;
	}

	@Override
	@Nullable
	public String getEntityGraphName() {
		return null;
	}

	@Override
	@Nonnull
	public Map<String, Object> getHints() {
		return Collections.emptyMap();
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
