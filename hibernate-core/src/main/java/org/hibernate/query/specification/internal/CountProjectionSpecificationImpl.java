/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
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
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 */
public class CountProjectionSpecificationImpl<T> implements SimpleProjectionSpecification<T,Long>, TypedQueryReference<Long> {

	private final SelectionSpecification<T> selectionSpecification;

	public CountProjectionSpecificationImpl(SelectionSpecification<T> specification) {
		this.selectionSpecification = specification;
	}

	@Override
	public QuerySpecification<T> restrict(Restriction<? super T> restriction) {
		throw new UnsupportedOperationException( "This is not supported yet!" );
	}

	@Override
	public SelectionQuery<Long> createQuery(Session session) {
		return session.createSelectionQuery( buildCriteria( session.getCriteriaBuilder() ) );
	}

	@Override
	public SelectionQuery<Long> createQuery(StatelessSession session) {
		return session.createSelectionQuery( buildCriteria( session.getCriteriaBuilder() ) );
	}

	@Override
	public SelectionQuery<Long> createQuery(EntityManager entityManager) {
		return entityManager.unwrap( SharedSessionContract.class )
				.createQuery( buildCriteria( entityManager.getCriteriaBuilder() ) );
	}

	@Override
	public CriteriaQuery<Long> buildCriteria(CriteriaBuilder builder) {
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

	@Override
	public SimpleProjectionSpecification<T,Long> validate(CriteriaBuilder builder) {
		selectionSpecification.validate( builder );
		return this;
	}

	@Override
	public TypedQueryReference<Long> reference() {
		return this;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Class<Long> getResultType() {
		return Long.class;
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return null;
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return null;
	}

	@Override
	public LockModeType getLockMode() {
		return null;
	}

	@Override
	public PessimisticLockScope getPessimisticLockScope() {
		return null;
	}

	@Override
	public String getEntityGraphName() {
		return null;
	}

	@Override
	public Map<String, Object> getHints() {
		return Collections.emptyMap();
	}

	@Override
	public List<Class<?>> getParameterTypes() {
		return List.of();
	}

	@Override
	public List<String> getParameterNames() {
		return List.of();
	}

	@Override
	public List<Object> getArguments() {
		return List.of();
	}

	@Override
	public Timeout getTimeout() {
		return null;
	}
}
