/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.programmatic.internal;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Order;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.programmatic.SelectionSpecification;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmSelectionQueryImpl;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.hibernate.query.sqm.tree.SqmCopyContext.noParamCopyContext;

/**
 * Standard implementation of SelectionSpecification
 *
 * @author Steve Ebersole
 */
public class SelectionSpecificationImpl<T> implements SelectionSpecification<T> {
	private final Class<T> resultType;
	private final SharedSessionContractImplementor session;

	private final SqmSelectStatement<T> sqmStatement;
	private final SqmRoot<T> sqmRoot;

	public SelectionSpecificationImpl(
			String hql,
			Class<T> resultType,
			SharedSessionContractImplementor session) {
		this.resultType = resultType;
		this.session = session;
		this.sqmStatement = resolveSqmTree( hql, resultType, session );
		this.sqmRoot = extractRoot( sqmStatement, resultType, hql );
	}

	public SelectionSpecificationImpl(
			Class<T> rootEntityType,
			SharedSessionContractImplementor session) {
		this( "from " + determineEntityName( rootEntityType, session ), rootEntityType, session );
	}

	@Override
	public Root<T> getRoot() {
		return sqmRoot;
	}

	@Override
	public CriteriaQuery<T> getCriteria() {
		return sqmStatement;
	}

	@Override
	public SelectionSpecification<T> addRestriction(Restriction<T> restriction) {
		final SqmPredicate sqmPredicate = SqmUtil.restriction( sqmStatement, resultType, restriction );
		sqmStatement.getQuerySpec().applyPredicate( sqmPredicate );

		return this;
	}

	@Override
	public SelectionSpecification<T> addOrdering(Order<T> order) {
		final SqmSortSpecification sortSpecification = SqmUtil.sortSpecification( sqmStatement, order );
		if ( sqmStatement.getQuerySpec().getOrderByClause() == null ) {
			sqmStatement.getQuerySpec().setOrderByClause( new SqmOrderByClause() );
		}
		sqmStatement.getQuerySpec().getOrderByClause().addSortSpecification( sortSpecification );

		return this;
	}

	@Override
	public final SelectionSpecification<T> setOrdering(Order<T> order) {
		sqmStatement.getQuerySpec().setOrderByClause( new SqmOrderByClause() );
		addOrdering( order );
		return this;
	}

	@Override
	public final SelectionSpecification<T> setOrdering(List<Order<T>> orders) {
		sqmStatement.getQuerySpec().setOrderByClause( new SqmOrderByClause() );
		orders.forEach( this::addOrdering );
		return this;
	}

	@Override
	public SelectionQuery<T> createQuery() {
		return new SqmSelectionQueryImpl<>( sqmStatement, true, resultType, session );
	}

	/**
	 * Used during construction to parse/interpret the incoming HQL
	 * and produce the corresponding SQM tree.
	 */
	private static <T> SqmSelectStatement<T> resolveSqmTree(
			String hql,
			Class<T> resultType,
			SharedSessionContractImplementor session) {
		final QueryEngine queryEngine = session.getFactory().getQueryEngine();
		final HqlInterpretation<T> hqlInterpretation = queryEngine
				.getInterpretationCache()
				.resolveHqlInterpretation( hql, resultType, queryEngine.getHqlTranslator() );

		if ( !SqmUtil.isSelect( hqlInterpretation.getSqmStatement() ) ) {
			throw new IllegalSelectQueryException( "Expecting a selection query, but found '" + hql + "'", hql );
		}
		hqlInterpretation.validateResultType( resultType );

		// NOTE: this copy is to isolate the actual AST tree from the
		// one stored in the interpretation cache
		return (SqmSelectStatement<T>) hqlInterpretation
				.getSqmStatement()
				.copy( noParamCopyContext( SqmQuerySource.CRITERIA ) );
	}

	/**
	 * Used during construction.  Mainly used to group extracting and
	 * validating the root.
	 */
	private SqmRoot<T> extractRoot(SqmSelectStatement<T> sqmStatement, Class<T> resultType, String hql) {
		final Set<SqmRoot<?>> sqmRoots = sqmStatement.getQuerySpec().getRoots();
		if ( sqmRoots.isEmpty() ) {
			throw new QueryException( "Query did not define any roots", hql );
		}
		if ( sqmRoots.size() > 1 ) {
			throw new QueryException( "Query defined multiple roots", hql );
		}
		final SqmRoot<?> sqmRoot = sqmRoots.iterator().next();
		if ( sqmRoot.getJavaType() != null
			&& !Map.class.isAssignableFrom( sqmRoot.getJavaType() )
			&& !resultType.isAssignableFrom( sqmRoot.getJavaType() ) ) {
			throw new QueryException(
					String.format(
							Locale.ROOT,
							"Query root [%s] and result type [%s] are not compatible",
							sqmRoot.getJavaType().getName(),
							resultType.getName()
					),
					hql
			);
		}
		//noinspection unchecked
		return (SqmRoot<T>) sqmRoot;
	}

	private static String determineEntityName(
			Class<?> rootEntityType,
			SharedSessionContractImplementor session) {
		final EntityDomainType<?> entityType = session
				.getFactory()
				.getJpaMetamodel()
				.findEntityType( rootEntityType );
		if ( entityType == null ) {
			return rootEntityType.getName();
		}
		return entityType.getName();
	}
}
