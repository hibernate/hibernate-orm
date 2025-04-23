/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.programmatic.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaBuilder;

import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Order;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.programmatic.SelectionSpecification;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmSelectionQueryImpl;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.hibernate.query.sqm.internal.SqmUtil.validateCriteriaQuery;
import static org.hibernate.query.sqm.tree.SqmCopyContext.noParamCopyContext;

/**
 * Standard implementation of SelectionSpecification
 *
 * @author Steve Ebersole
 */
public class SelectionSpecificationImpl<T> implements SelectionSpecification<T> {
	private final Class<T> resultType;
	private final String hql;
	private final CriteriaQuery<T> criteriaQuery;
	private final List<BiConsumer<SqmSelectStatement<T>, SqmRoot<T>>> specifications = new ArrayList<>();

	public SelectionSpecificationImpl(Class<T> resultType) {
		this.resultType = resultType;
		this.hql = null;
		this.criteriaQuery = null;
	}

	public SelectionSpecificationImpl(String hql, Class<T> resultType) {
		this.resultType = resultType;
		this.hql = hql;
		this.criteriaQuery = null;
	}

	public SelectionSpecificationImpl(CriteriaQuery<T> criteriaQuery) {
		this.resultType = criteriaQuery.getResultType();
		this.hql = null;
		this.criteriaQuery = criteriaQuery;
	}

	@Override
	public SelectionSpecification<T> restrict(Restriction<? super T> restriction) {
		specifications.add( (sqmStatement, root) -> {
			final SqmPredicate sqmPredicate = SqmUtil.restriction( sqmStatement, resultType, restriction );
			sqmStatement.getQuerySpec().applyPredicate( sqmPredicate );
		} );
		return this;
	}

	@Override
	public SelectionSpecification<T> augment(Augmentation<T> augmentation) {
		specifications.add( (sqmStatement, root) ->
				augmentation.augment( sqmStatement.nodeBuilder(), sqmStatement, root ) );
		return this;
	}

	@Override
	public SelectionSpecification<T> fetch(Path<T, ?> fetchPath) {
		specifications.add( (sqmStatement, root) -> fetchPath.fetch( root ) );
		return this;
	}

	@Override
	public SelectionSpecification<T> sort(Order<? super T> order) {
		specifications.add( (sqmStatement, root) -> {
			addOrder( order, sqmStatement );
		} );
		return this;
	}

	@Override
	public final SelectionSpecification<T> resort(Order<? super T> order) {
		specifications.add( (sqmStatement, root) -> {
			sqmStatement.getQuerySpec().setOrderByClause( new SqmOrderByClause() );
			addOrder( order, sqmStatement );
		} );
		return this;
	}

	@Override
	public final SelectionSpecification<T> resort(List<Order<? super T>> orders) {
		specifications.add( (sqmStatement, root) -> {
			sqmStatement.getQuerySpec().setOrderByClause( new SqmOrderByClause() );
			orders.forEach( order -> addOrder( order, sqmStatement ) );
		} );
		return this;
	}

	private static <T> void addOrder(Order<? super T> order, SqmSelectStatement<T> sqmStatement) {
		final SqmSortSpecification sortSpecification = SqmUtil.sortSpecification( sqmStatement, order );
		final var querySpec = sqmStatement.getQuerySpec();
		if ( querySpec.getOrderByClause() == null ) {
			querySpec.setOrderByClause( new SqmOrderByClause() );
		}
		querySpec.getOrderByClause().addSortSpecification( sortSpecification );
	}

	@Override
	public SelectionQuery<T> createQuery(Session session) {
		return createQuery( (SharedSessionContract) session );
	}

	@Override
	public SelectionQuery<T> createQuery(StatelessSession session) {
		return createQuery( (SharedSessionContract) session );
	}

	public SelectionQuery<T> createQuery(SharedSessionContract session) {
		final var sessionImpl = (SharedSessionContractImplementor) session;
		final SqmSelectStatement<T> sqmStatement = build( sessionImpl.getFactory().getQueryEngine() );
		return new SqmSelectionQueryImpl<>( sqmStatement, true, resultType, sessionImpl );
	}

	private SqmSelectStatement<T> build(QueryEngine queryEngine) {
		final SqmSelectStatement<T> sqmStatement;
		final SqmRoot<T> sqmRoot;
		if ( hql != null ) {
			sqmStatement = resolveSqmTree( hql, resultType, queryEngine );
			sqmRoot = extractRoot( sqmStatement, resultType, hql );
		}
		else if ( criteriaQuery != null ) {
			sqmStatement = (SqmSelectStatement<T>) criteriaQuery;
			sqmRoot = extractRoot( sqmStatement, resultType, "criteria query" );
		}
		else {
			var builder = queryEngine.getCriteriaBuilder();
			var query = builder.createQuery( resultType );
			var root = query.from( resultType );
			query.select( root );
			sqmRoot = (SqmRoot<T>) root;
			sqmStatement = (SqmSelectStatement<T>) query;
		}
		specifications.forEach( consumer -> consumer.accept( sqmStatement, sqmRoot ) );
		return sqmStatement;
	}

	@Override
	public SelectionQuery<T> createQuery(EntityManager entityManager) {
		return createQuery( (SharedSessionContract) entityManager );
	}

	@Override
	public CriteriaQuery<T> buildCriteria(CriteriaBuilder builder) {
		final NodeBuilder nodeBuilder = (NodeBuilder) builder;
		return build( nodeBuilder.getQueryEngine() );
	}

	@Override
	public SelectionSpecification<T> validate(CriteriaBuilder builder) {
		final NodeBuilder nodeBuilder = (NodeBuilder) builder;
		final var statement = build( nodeBuilder.getQueryEngine() );
		final var queryPart = statement.getQueryPart();
		// For criteria queries, we have to validate the fetch structure here
		queryPart.validateQueryStructureAndFetchOwners();
		validateCriteriaQuery( queryPart );
		statement.validateResultType( resultType );
		return this;
	}

	/**
	 * Used during construction to parse/interpret the incoming HQL
	 * and produce the corresponding SQM tree.
	 */
	private static <T> SqmSelectStatement<T> resolveSqmTree(String hql, Class<T> resultType, QueryEngine queryEngine) {
		final HqlInterpretation<T> hqlInterpretation =
				queryEngine.getInterpretationCache()
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
}
