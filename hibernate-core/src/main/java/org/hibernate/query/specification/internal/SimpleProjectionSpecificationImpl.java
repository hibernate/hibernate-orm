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
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.SharedSessionContract;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.QuerySpecification;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.query.specification.SimpleProjectionSpecification;
import org.hibernate.query.spi.JpaTypedQueryReference;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;

import java.util.Collections;
import java.util.Map;

/**
 * @author Gavin King
 */
public class SimpleProjectionSpecificationImpl<T,X>
		implements SimpleProjectionSpecification<T,X>, JpaTypedQueryReference<X> {

	private final SelectionSpecification<T> selectionSpecification;
	private final Path<T, X> path;
	private final SingularAttribute<? super T, X> attribute;

	public SimpleProjectionSpecificationImpl(
			@Nonnull SelectionSpecification<T> specification,
			@Nonnull Path<T, X> path) {
		this.selectionSpecification = specification;
		this.path = path;
		this.attribute = null;
	}

	public SimpleProjectionSpecificationImpl(
			@Nonnull SelectionSpecification<T> specification,
			@Nonnull SingularAttribute<? super T, X> attribute) {
		this.selectionSpecification = specification;
		this.attribute = attribute;
		this.path = null;
	}

	@Nonnull
	@Override
	public QuerySpecification<T> restrict(@Nonnull Restriction<? super T> restriction) {
		throw new UnsupportedOperationException( "This is not supported yet!" );
	}

	@Nonnull
	@Override
	public SelectionQuery<X> createQuery(@Nonnull EntityHandler entityHandler) {
		return entityHandler.unwrap( SharedSessionContract.class )
				.createQuery( buildCriteria( entityHandler.getCriteriaBuilder() ) );
	}

	@Nonnull
	@Override
	public CriteriaQuery<X> buildCriteria(@Nonnull CriteriaBuilder builder) {
		var impl = (SelectionSpecificationImpl<T>) selectionSpecification;
		// TODO: handle HQL, existing criteria
		final var tupleQuery =
				(SqmSelectStatement<X>)
						builder.createQuery( getResultType() );
		final var root = tupleQuery.from( impl.getResultType() );
		// This cast is completely bogus
		final var castStatement = (SqmSelectStatement<T>) tupleQuery;
		impl.getSpecifications().forEach( spec -> spec.accept( castStatement, root ) );
		if ( path != null ) {
			tupleQuery.select( path.path( root ) );
		}
		else if ( attribute != null ) {
			tupleQuery.select( root.get( attribute ) );
		}
		return tupleQuery;
	}

	@Nonnull
	@Override
	public SimpleProjectionSpecification<T,X> validate(@Nonnull CriteriaBuilder builder) {
		selectionSpecification.validate( builder );
		// TODO: validate projection
		return this;
	}

	@Nonnull
	@Override
	public TypedQueryReference<X> reference() {
		return this;
	}

	@Override
	@Nullable
	public String getName() {
		return null;
	}

	@Override
	@Nonnull
	public Class<X> getResultType() {
		if ( path != null ) {
			return path.getType();
		}
		else if ( attribute != null ) {
			return attribute.getJavaType();
		}
		else {
			throw new IllegalStateException( "No path or attribute" );
		}
	}

	@Override
	@Nonnull
	public Map<String, Object> getHints() {
		return Collections.emptyMap();
	}

	@Override
	@Nullable
	public Timeout getTimeout() {
		return null;
	}

	@Override
	@Nonnull
	public String getEntityGraphName() {
		return "";
	}
}
