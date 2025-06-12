/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.CommonAbstractCriteria;

import org.hibernate.AssertionFailure;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.hibernate.query.sqm.tree.SqmCopyContext.noParamCopyContext;

/**
 * Standard implementation of MutationSpecification
 *
 * @author Steve Ebersole
 */
public class MutationSpecificationImpl<T> implements MutationSpecification<T>, TypedQueryReference<Void> {

	private final List<BiConsumer<SqmDeleteOrUpdateStatement<T>, SqmRoot<T>>> specifications = new ArrayList<>();
	private final String hql;
	private final Class<T> mutationTarget;
	private final SqmDeleteOrUpdateStatement<T> deleteOrUpdateStatement;

	public MutationSpecificationImpl(String hql, Class<T> mutationTarget) {
		this.hql = hql;
		this.mutationTarget = mutationTarget;
		this.deleteOrUpdateStatement = null;
	}

	public MutationSpecificationImpl(CriteriaUpdate<T> criteriaQuery) {
		this.deleteOrUpdateStatement = (SqmUpdateStatement<T>) criteriaQuery;
		this.mutationTarget = deleteOrUpdateStatement.getTarget().getManagedType().getJavaType();
		this.hql = null;
	}

	public MutationSpecificationImpl(CriteriaDelete<T> criteriaQuery) {
		this.deleteOrUpdateStatement = (SqmDeleteStatement<T>) criteriaQuery;
		this.mutationTarget = deleteOrUpdateStatement.getTarget().getManagedType().getJavaType();
		this.hql = null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Class<Void> getResultType() {
		return null;
	}

	@Override
	public Map<String,Object> getHints() {
		return Collections.emptyMap();
	}

	@Override
	public TypedQueryReference<Void> reference() {
		return this;
	}

	@Override
	public MutationSpecification<T> restrict(Restriction<? super T> restriction) {
		specifications.add( (sqmStatement, mutationTargetRoot) -> {
			final SqmPredicate sqmPredicate = (SqmPredicate) restriction.toPredicate(
					mutationTargetRoot,
					sqmStatement.nodeBuilder()
			);
			sqmStatement.applyPredicate( sqmPredicate );
		} );
		return this;
	}

	@Override
	public MutationSpecification<T> augment(Augmentation<T> augmentation) {
		specifications.add( (sqmStatement, mutationTargetRoot) ->
				augmentation.augment( sqmStatement.nodeBuilder(), sqmStatement, mutationTargetRoot ) );
		return this;
	}

	@Override
	public MutationQuery createQuery(Session session) {
		return createQuery( (SharedSessionContract) session );
	}

	@Override
	public MutationQuery createQuery(StatelessSession session) {
		return createQuery( (SharedSessionContract) session );
	}

	public MutationQuery createQuery(SharedSessionContract session) {
		final var sessionImpl = session.unwrap(SharedSessionContractImplementor.class);
		final SqmDeleteOrUpdateStatement<T> sqmStatement = build( sessionImpl.getFactory().getQueryEngine() );
		return new QuerySqmImpl<>( sqmStatement, true, null, sessionImpl );
	}

	private SqmDeleteOrUpdateStatement<T> build(QueryEngine queryEngine) {
		final SqmDeleteOrUpdateStatement<T> sqmStatement;
		final SqmRoot<T> mutationTargetRoot;
		if ( hql != null ) {
			sqmStatement = resolveSqmTree( hql, queryEngine );
			mutationTargetRoot = resolveSqmRoot( sqmStatement, mutationTarget );
		}
		else if ( deleteOrUpdateStatement != null ) {
			sqmStatement = deleteOrUpdateStatement;
			mutationTargetRoot = resolveSqmRoot( sqmStatement,
					sqmStatement.getTarget().getManagedType().getJavaType() );
		}
		else {
			throw new AssertionFailure( "No HQL or criteria" );
		}
		specifications.forEach( consumer -> consumer.accept( sqmStatement, mutationTargetRoot ) );
		return sqmStatement;
	}

	@Override
	public MutationQuery createQuery(EntityManager entityManager) {
		return createQuery( (SharedSessionContract) entityManager );
	}

	@Override
	public CommonAbstractCriteria buildCriteria(CriteriaBuilder builder) {
		final NodeBuilder nodeBuilder = (NodeBuilder) builder;
		return build( nodeBuilder.getQueryEngine() );
	}

	@Override
	public MutationSpecification<T> validate(CriteriaBuilder builder) {
		final NodeBuilder nodeBuilder = (NodeBuilder) builder;
		final var statement = build( nodeBuilder.getQueryEngine() );
		( (AbstractSqmDmlStatement<?>) statement ).validate( hql );
		return this;
	}

	/**
	 * Used during construction to parse/interpret the incoming HQL
	 * and produce the corresponding SQM tree.
	 */
	private static <T> SqmDeleteOrUpdateStatement<T> resolveSqmTree(String hql, QueryEngine queryEngine) {
		final HqlInterpretation<T> hqlInterpretation =
				queryEngine.getInterpretationCache()
						.resolveHqlInterpretation( hql, null, queryEngine.getHqlTranslator() );

		if ( !SqmUtil.isRestrictedMutation( hqlInterpretation.getSqmStatement() ) ) {
			throw new IllegalMutationQueryException( "Expecting a delete or update query, but found '" + hql + "'", hql);
		}

		// NOTE: this copy is to isolate the actual AST tree from the
		// one stored in the interpretation cache
		return (SqmDeleteOrUpdateStatement<T>) hqlInterpretation
				.getSqmStatement()
				.copy( noParamCopyContext( SqmQuerySource.CRITERIA ) );
	}

	/**
	 * Used during construction.  Mainly used to group extracting and
	 * validating the root.
	 */
	private static <T> SqmRoot<T> resolveSqmRoot(
			SqmDeleteOrUpdateStatement<T> sqmStatement,
			Class<T> mutationTarget) {
		final SqmRoot<T> mutationTargetRoot = sqmStatement.getTarget();
		if ( mutationTargetRoot.getJavaType() != null
			&& !mutationTarget.isAssignableFrom( mutationTargetRoot.getJavaType() ) ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Mutation target types do not match : %s / %s",
							mutationTargetRoot.getJavaType().getName(),
							mutationTarget.getName()
					)
			);
		}
		return mutationTargetRoot;
	}
}
