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
import org.hibernate.query.specification.ProjectionSpecification;
import org.hibernate.query.specification.QuerySpecification;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.query.spi.JpaTypedQueryReference;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.spi.select.SqmSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.Collections.emptyMap;
import static org.hibernate.internal.util.type.PrimitiveWrappers.cast;

/**
 * @author Gavin King
 */
public class ProjectionSpecificationImpl<T> implements ProjectionSpecification<T>, JpaTypedQueryReference<Object[]> {

	private final SelectionSpecification<T> selectionSpecification;
	private final List<BiFunction<SqmSelectStatement<Object[]>, SqmRoot<T>, SqmSelectableNode<?>>> specifications = new ArrayList<>();

	public ProjectionSpecificationImpl(@Nonnull SelectionSpecification<T> selectionSpecification) {
		this.selectionSpecification = selectionSpecification;
	}

	@Nonnull
	@Override
	public <X> Element<X> select(@Nonnull SingularAttribute<T, X> attribute) {
		final int position = specifications.size();
		specifications.add( (select, root) -> root.get( attribute ) );
		return tuple -> cast( attribute.getJavaType(), tuple[position] );
	}

	@Nonnull
	@Override
	public <X> Element<X> select(@Nonnull Path<T, X> path) {
		final int position = specifications.size();
		specifications.add( (select, root) -> (SqmPath<X>) path.path( root ) );
		return tuple -> cast( path.getType(), tuple[position] );
	}

	@Nonnull
	@Override
	public QuerySpecification<Object[]> restrict(@Nonnull Restriction<? super Object[]> restriction) {
		throw new UnsupportedOperationException( "This is not supported yet!" );
	}

	@Nonnull
	@Override
	public SelectionQuery<Object[]> createQuery(@Nonnull EntityHandler entityHandler) {
		return entityHandler.unwrap( SharedSessionContract.class )
				.createQuery( buildCriteria( entityHandler.getCriteriaBuilder() ) );
	}

	@Nonnull
	@Override
	public CriteriaQuery<Object[]> buildCriteria(@Nonnull CriteriaBuilder builder) {
		var impl = (SelectionSpecificationImpl<T>) selectionSpecification;
		// TODO: handle HQL, existing criteria
		final var tupleQuery =
				(SqmSelectStatement<Object[]>)
						builder.createQuery(Object[].class);
		final var root = tupleQuery.from( impl.getResultType() );
		// This cast is completely bogus
		final var castStatement = (SqmSelectStatement<T>) tupleQuery;
		impl.getSpecifications().forEach( spec -> spec.accept( castStatement, root ) );
		final var nodeBuilder = (NodeBuilder) builder;
		final var selectClause = tupleQuery.getQuerySpec().getSelectClause();
		for ( int i = 0; i < specifications.size(); i++ ) {
			final var selection = specifications.get( i ).apply( tupleQuery, root );
			selectClause.addSelection( new SqmSelection<>( selection, selection.getAlias(), nodeBuilder ) );
		}
		return tupleQuery;
	}

	@Nonnull
	@Override
	public ProjectionSpecification<T> validate(@Nonnull CriteriaBuilder builder) {
		selectionSpecification.validate( builder );
		// TODO: validate projection
		return this;
	}

	@Nonnull
	@Override
	public TypedQueryReference<Object[]> reference() {
		return this;
	}

	@Override
	@Nullable
	public String getName() {
		return null;
	}

	@Override
	@Nonnull
	public Class<Object[]> getResultType() {
		return Object[].class;
	}

	@Override
	@Nonnull
	public Map<String, Object> getHints() {
		return emptyMap();
	}

	@Override
	@Nullable
	public Timeout getTimeout() {
		return null;
	}

	@Override
	@Nullable
	public String getEntityGraphName() {
		return null;
	}
}
