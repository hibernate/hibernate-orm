/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.QuerySpecification;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.query.specification.SimpleProjectionSpecification;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import java.util.Collections;
import java.util.Map;

/**
 * @author Gavin King
 */
public class ExistsProjectionSpecificationImpl<T> implements SimpleProjectionSpecification<T,Boolean>, TypedQueryReference<Boolean> {

	private final SelectionSpecification<T> selectionSpecification;

	public ExistsProjectionSpecificationImpl(SelectionSpecification<T> specification) {
		this.selectionSpecification = specification;
	}

	@Override
	public QuerySpecification<T> restrict(Restriction<? super T> restriction) {
		throw new UnsupportedOperationException( "This is not supported yet!" );
	}

	@Override
	public SelectionQuery<Boolean> createQuery(Session session) {
		return session.createSelectionQuery( buildCriteria( session.getCriteriaBuilder() ) );
	}

	@Override
	public SelectionQuery<Boolean> createQuery(StatelessSession session) {
		return session.createSelectionQuery( buildCriteria( session.getCriteriaBuilder() ) );
	}

	@Override
	public SelectionQuery<Boolean> createQuery(EntityManager entityManager) {
		return entityManager.unwrap( SharedSessionContract.class )
				.createQuery( buildCriteria( entityManager.getCriteriaBuilder() ) );
	}

	@Override
	public CriteriaQuery<Boolean> buildCriteria(CriteriaBuilder builder) {
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

	@Override
	public SimpleProjectionSpecification<T,Boolean> validate(CriteriaBuilder builder) {
		selectionSpecification.validate( builder );
		return this;
	}

	@Override
	public TypedQueryReference<Boolean> reference() {
		return this;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Class<Boolean> getResultType() {
		return Boolean.class;
	}

	@Override
	public Map<String, Object> getHints() {
		return Collections.emptyMap();
	}
}
