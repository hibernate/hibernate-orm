/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementReference;
import jakarta.persistence.Timeout;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.criteria.CriteriaUpdate;
import org.hibernate.AssertionFailure;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.internal.MutationQueryImpl;
import org.hibernate.query.named.NamedMutationMemento;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.JpaStatementReference;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hibernate.internal.util.ArgumentsHelper.bindReferenceArguments;
import static org.hibernate.query.sqm.tree.SqmCopyContext.noParamCopyContext;
import static org.hibernate.query.sqm.tree.SqmCopyContext.simpleContext;

/**
 * Standard implementation of {@link MutationSpecification}.
 *
 * @author Steve Ebersole
 */
public class MutationSpecificationImpl<T> implements MutationSpecification<T>, JpaStatementReference<T> {

	public enum MutationType {
//		INSERT,
		UPDATE,
		DELETE
	}

	final List<BiConsumer<SqmDeleteOrUpdateStatement<T>, SqmRoot<T>>> specifications = new ArrayList<>();

	private final String hql;
	private final Class<T> mutationTarget;
	private final SqmDeleteOrUpdateStatement<T> deleteOrUpdateStatement;
	private final MutationType type;
	private final StatementReference statementReference;

	public MutationSpecificationImpl(String hql, Class<T> mutationTarget) {
		this.hql = hql;
		this.mutationTarget = mutationTarget;
		this.deleteOrUpdateStatement = null;
		this.type = null;
		this.statementReference = null;
	}

	public MutationSpecificationImpl(CriteriaUpdate<T> criteriaQuery) {
		this.deleteOrUpdateStatement = (SqmUpdateStatement<T>) criteriaQuery;
		this.mutationTarget = deleteOrUpdateStatement.getTarget().getManagedType().getJavaType();
		this.hql = null;
		this.type = MutationType.UPDATE;
		this.statementReference = null;
	}

	public MutationSpecificationImpl(CriteriaDelete<T> criteriaQuery) {
		this.deleteOrUpdateStatement = (SqmDeleteStatement<T>) criteriaQuery;
		this.mutationTarget = deleteOrUpdateStatement.getTarget().getManagedType().getJavaType();
		this.hql = null;
		this.type = MutationType.DELETE;
		this.statementReference = null;
	}

	public MutationSpecificationImpl(MutationType type, Class<T> mutationTarget) {
		this.deleteOrUpdateStatement = null;
		this.mutationTarget = mutationTarget;
		this.hql = null;
		this.type = type;
		this.statementReference = null;
	}

	public MutationSpecificationImpl(StatementReference statementReference) {
		this.deleteOrUpdateStatement = null;
		this.mutationTarget = null;
		this.hql = null;
		this.type = null;
		this.statementReference = statementReference;
	}

	@Override
	public String getName() {
		return statementReference == null ? null : statementReference.getName();
	}

	@Override
	public Map<String,Object> getHints() {
		return statementReference == null ? emptyMap() : statementReference.getHints();
	}

	@Override
	public Timeout getTimeout() {
		return statementReference instanceof JpaStatementReference<?> jpaStatementReference
				? jpaStatementReference.getTimeout()
				: null;
	}

	@Override
	public List<Class<?>> getParameterTypes() {
		return statementReference == null ? null : statementReference.getParameterTypes();
	}

	@Override
	public List<String> getParameterNames() {
		return statementReference == null ? null : statementReference.getParameterNames();
	}

	@Override
	public List<Object> getArguments() {
		return statementReference == null ? null : statementReference.getArguments();
	}

	@Override
	public Set<Statement.Option> getOptions() {
		return statementReference == null ? emptySet() : statementReference.getOptions();
	}

	@Override
	public StatementReference reference() {
		return this;
	}

	@Override
	public MutationSpecification<T> restrict(Restriction<? super T> restriction) {
		specifications.add( (sqmStatement, mutationTargetRoot) -> {
			final var sqmPredicate = (SqmPredicate)
					restriction.toPredicate( mutationTargetRoot,
							sqmStatement.nodeBuilder() );
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
	public MutationQueryImplementor<T> createQuery(Session session) {
		return createQuery( (SharedSessionContract) session );
	}

	@Override
	public MutationQueryImplementor<T> createQuery(StatelessSession session) {
		return createQuery( (SharedSessionContract) session );
	}

	public MutationQueryImplementor<T> createQuery(SharedSessionContract session) {
		final var sessionImpl = session.unwrap(SharedSessionContractImplementor.class);
		final var buildResult = build( sessionImpl.getFactory().getQueryEngine() );
		final var query =
				buildResult.sqmMemento == null
						? new MutationQueryImpl<>( buildResult.sqmStatement, false, sessionImpl )
						: new MutationQueryImpl<>( buildResult.sqmMemento, buildResult.sqmStatement, false, sessionImpl );
		if ( statementReference != null ) {
			bindReferenceArguments( query, statementReference );
			setHintsAndOptions( query ); // arguably unnecessary
		}
		return query;
	}

	private void setHintsAndOptions(MutationQueryImpl<T> query) {
		final var hints = statementReference.getHints();
		if ( hints != null ) {
			hints.forEach( query::setHint );
		}
		statementReference.getOptions().forEach( query::addOption );
	}


	private record SqmBuildResult<T>(
			SqmDeleteOrUpdateStatement<T> sqmStatement,
			NamedSqmQueryMemento<?> sqmMemento) {
	}

	private SqmBuildResult<T> build(QueryEngine queryEngine) {
		final SqmDeleteOrUpdateStatement<T> sqmStatement;
		final SqmRoot<T> mutationTargetRoot;
		final NamedSqmQueryMemento<?> sqmMemento;
		if ( hql != null ) {
			sqmStatement = resolveSqmTree( hql, queryEngine );
			mutationTargetRoot = resolveSqmRoot( sqmStatement, mutationTarget );
			sqmMemento = null;
		}
		else if ( deleteOrUpdateStatement != null ) {
			sqmStatement = (SqmDeleteOrUpdateStatement<T>)
					deleteOrUpdateStatement.copy( simpleContext() );
			mutationTargetRoot = resolveSqmRoot( sqmStatement,
					sqmStatement.getTarget().getManagedType().getJavaType() );
			sqmMemento = null;
		}
		else if ( type != null ) {
			final var criteriaBuilder = queryEngine.getCriteriaBuilder();
			sqmStatement = switch ( type ) {
				case UPDATE -> criteriaBuilder.createCriteriaUpdate( mutationTarget );
				case DELETE -> criteriaBuilder.createCriteriaDelete( mutationTarget );
			};
			mutationTargetRoot = sqmStatement.getTarget();
			sqmMemento = null;
		}
		else if ( statementReference != null ) {
			if ( statementReference instanceof MutationSpecification<?> mutationSpecification ) {
				//noinspection unchecked
				sqmStatement = (SqmDeleteOrUpdateStatement<T>)
						mutationSpecification.buildCriteria( queryEngine.getCriteriaBuilder() );
				mutationTargetRoot = sqmStatement.getTarget();
				sqmMemento = null;
			}
			else {
				sqmMemento = resolveSqmMutationMemento( statementReference, queryEngine );
				sqmStatement = resolveSqmTree( sqmMemento, queryEngine );
				mutationTargetRoot = sqmStatement.getTarget();
			}
		}
		else {
			throw new AssertionFailure( "No HQL or criteria" );
		}
		specifications.forEach( consumer -> consumer.accept( sqmStatement, mutationTargetRoot ) );
		return new SqmBuildResult<>( sqmStatement, sqmMemento );
	}

	@Override
	public MutationQueryImplementor<T> createQuery(EntityManager entityManager) {
		return createQuery( (SharedSessionContract) entityManager );
	}

	@Override
	public CriteriaStatement<T> buildCriteria(CriteriaBuilder builder) {
		final var nodeBuilder = (NodeBuilder) builder;
		return build( nodeBuilder.getQueryEngine() ).sqmStatement;
	}

	@Override
	public MutationSpecification<T> validate(CriteriaBuilder builder) {
		final var nodeBuilder = (NodeBuilder) builder;
		final var statement = build( nodeBuilder.getQueryEngine() ).sqmStatement;
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
						// FIXME : unchecked cast
						.resolveHqlInterpretation( hql, null, queryEngine.getHqlTranslator() );
		if ( SqmUtil.isRestrictedMutation( hqlInterpretation.getSqmStatement() ) ) {
			// NOTE: this copy is to isolate the actual AST tree from the
			// one stored in the interpretation cache
			return (SqmDeleteOrUpdateStatement<T>)
					hqlInterpretation.getSqmStatement()
							.copy( noParamCopyContext( SqmQuerySource.CRITERIA ) );
		}
		else {
			throw new IllegalMutationQueryException( "Expecting a delete or update query, but found '" + hql + "'",
					hql );
		}
	}

	/**
	 * Used during construction to resolve an incoming named statement reference
	 * and produce the corresponding SQM tree.
	 */
	private static <T> SqmDeleteOrUpdateStatement<T> resolveSqmTree(
			NamedSqmQueryMemento<?> sqmMemento,
			QueryEngine queryEngine) {
		final var sqmStatement = sqmMemento.getSqmStatement();
		if ( sqmStatement == null ) {
			return resolveSqmTree( sqmMemento.getHqlString(), queryEngine );
		}
		else if ( SqmUtil.isRestrictedMutation( sqmStatement ) ) {
			//noinspection unchecked
			return (SqmDeleteOrUpdateStatement<T>) sqmStatement.copy( simpleContext() );
		}
		else {
			throw new IllegalMutationQueryException(
					"Expecting a delete or update query, but found '" + sqmMemento.getHqlString() + "'",
					sqmMemento.getHqlString()
			);
		}

	}

	private static <T> NamedSqmQueryMemento<T> resolveSqmMutationMemento(
			StatementReference statementReference,
			QueryEngine queryEngine) {
		final NamedQueryMemento<T> namedMemento =
				queryEngine.getNamedObjectRepository()
						// FIXME: unchecked cast
						.getQueryMementoByName( statementReference.getName(), false );
		if ( namedMemento instanceof NamedMutationMemento<?>
				&& namedMemento instanceof NamedSqmQueryMemento<T> sqmMemento ) {
			return sqmMemento;
		}
		else {
			throw new IllegalMutationQueryException(
					"MutationSpecification only supports HQL or criteria statement references: "
							+ statementReference.getName()
			);
		}
	}

	/**
	 * Used during construction. Mainly used to group extracting and
	 * validating the root.
	 */
	private static <T> SqmRoot<T> resolveSqmRoot(
			SqmDeleteOrUpdateStatement<T> sqmStatement,
			Class<T> mutationTarget) {
		final var mutationTargetRoot = sqmStatement.getTarget();
		final var javaType = mutationTargetRoot.getJavaType();
		if ( javaType != null && !mutationTarget.isAssignableFrom( javaType ) ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Mutation target types do not match : %s / %s",
							javaType.getName(),
							mutationTarget.getName()
					)
			);
		}
		else {
			return mutationTargetRoot;
		}
	}

}
