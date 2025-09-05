/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.restriction.Path;
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
public class SimpleProjectionSpecificationImpl<T,X> implements SimpleProjectionSpecification<T,X>, TypedQueryReference<X> {

	private final SelectionSpecification<T> selectionSpecification;
	private final Path<T, X> path;
	private final SingularAttribute<T, X> attribute;

	public SimpleProjectionSpecificationImpl(SelectionSpecification<T> specification, Path<T, X> path) {
		this.selectionSpecification = specification;
		this.path = path;
		this.attribute = null;
	}

	public SimpleProjectionSpecificationImpl(SelectionSpecification<T> specification, SingularAttribute<T, X> attribute) {
		this.selectionSpecification = specification;
		this.attribute = attribute;
		this.path = null;
	}

	@Override
	public QuerySpecification<T> restrict(Restriction<? super T> restriction) {
		throw new UnsupportedOperationException( "This is not supported yet!" );
	}

	@Override
	public SelectionQuery<X> createQuery(Session session) {
		return session.createSelectionQuery( buildCriteria( session.getCriteriaBuilder() ) );
	}

	@Override
	public SelectionQuery<X> createQuery(StatelessSession session) {
		return session.createSelectionQuery( buildCriteria( session.getCriteriaBuilder() ) );
	}

	@Override
	public SelectionQuery<X> createQuery(EntityManager entityManager) {
		return entityManager.unwrap( SharedSessionContract.class )
				.createQuery( buildCriteria( entityManager.getCriteriaBuilder() ) );
	}

	@Override
	public CriteriaQuery<X> buildCriteria(CriteriaBuilder builder) {
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

	@Override
	public SimpleProjectionSpecification<T,X> validate(CriteriaBuilder builder) {
		selectionSpecification.validate( builder );
		// TODO: validate projection
		return this;
	}

	@Override
	public TypedQueryReference<X> reference() {
		return this;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
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
	public Map<String, Object> getHints() {
		return Collections.emptyMap();
	}
}
