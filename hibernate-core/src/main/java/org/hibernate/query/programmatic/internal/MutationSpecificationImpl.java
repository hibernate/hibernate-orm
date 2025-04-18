/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.programmatic.internal;

import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.programmatic.MutationSpecification;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

import java.util.Locale;

import static org.hibernate.query.sqm.tree.SqmCopyContext.noParamCopyContext;

/**
 * Standard implementation of MutationSpecification
 *
 * @author Steve Ebersole
 */
public class MutationSpecificationImpl<T> implements MutationSpecification<T> {

	private final SqmDeleteOrUpdateStatement<T> sqmStatement;
	private final SqmRoot<T> mutationTargetRoot;

	public MutationSpecificationImpl(
			String hql,
			Class<T> mutationTarget,
			SessionFactoryImplementor factory) {
		this.sqmStatement = resolveSqmTree( hql, factory.getQueryEngine() );
		this.mutationTargetRoot = resolveSqmRoot( this.sqmStatement, mutationTarget );
	}

	public MutationSpecificationImpl(CriteriaUpdate<T> criteriaQuery) {
		this.sqmStatement = (SqmUpdateStatement<T>) criteriaQuery;
		this.mutationTargetRoot = resolveSqmRoot( sqmStatement,
				sqmStatement.getTarget().getManagedType().getJavaType() );
	}

	public MutationSpecificationImpl(CriteriaDelete<T> criteriaQuery) {
		this.sqmStatement = (SqmDeleteStatement<T>) criteriaQuery;
		this.mutationTargetRoot = resolveSqmRoot( sqmStatement,
				sqmStatement.getTarget().getManagedType().getJavaType() );
	}

	@Override
	public Root<T> getRoot() {
		return mutationTargetRoot;
	}

	@Override
	public CommonAbstractCriteria getCriteria() {
		return sqmStatement;
	}

	@Override
	public MutationSpecification<T> addRestriction(Restriction<T> restriction) {
		final SqmPredicate sqmPredicate = (SqmPredicate) restriction.toPredicate(
				mutationTargetRoot,
				sqmStatement.nodeBuilder()
		);
		sqmStatement.applyPredicate( sqmPredicate );

		return this;
	}

	@Override
	public MutationQuery createQuery(SharedSessionContract session) {
		return new QuerySqmImpl<>( sqmStatement, true, null,
				(SharedSessionContractImplementor) session );
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
