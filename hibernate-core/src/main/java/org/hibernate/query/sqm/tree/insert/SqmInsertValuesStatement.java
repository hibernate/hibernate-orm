/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.insert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.query.criteria.JpaConflictClause;
import org.hibernate.query.criteria.JpaCriteriaInsertValues;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaValues;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.ValueBindJpaCriteriaParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

import jakarta.persistence.criteria.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Gavin King
 */
public class SqmInsertValuesStatement<T> extends AbstractSqmInsertStatement<T> implements JpaCriteriaInsertValues<T> {
	private @Nullable List<SqmValues> valuesList;

	public SqmInsertValuesStatement(SqmRoot<T> targetRoot, NodeBuilder nodeBuilder) {
		super( targetRoot, SqmQuerySource.HQL, nodeBuilder );
	}

	public SqmInsertValuesStatement(Class<T> targetEntity, NodeBuilder nodeBuilder) {
		super(
				new SqmRoot<>(
						nodeBuilder.getDomainModel().entity( targetEntity ),
						null,
						false,
						nodeBuilder
				),
				SqmQuerySource.CRITERIA,
				nodeBuilder
		);
	}

	private SqmInsertValuesStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmRoot<T> target,
			List<SqmPath<?>> insertionTargetPaths,
			SqmConflictClause<T> conflictClause,
			List<SqmValues> valuesList) {
		super( builder, querySource, parameters, cteStatements, target, insertionTargetPaths, conflictClause );
		this.valuesList = valuesList;
	}

	@Override
	public SqmInsertValuesStatement<T> copy(SqmCopyContext context) {
		final SqmInsertValuesStatement<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmValues> valuesList;
		if ( this.valuesList == null ) {
			valuesList = null;
		}
		else {
			valuesList = new ArrayList<>( this.valuesList.size() );
			for ( SqmValues sqmValues : this.valuesList ) {
				valuesList.add( sqmValues.copy( context ) );
			}
		}
		return context.registerCopy(
				this,
				new SqmInsertValuesStatement<>(
						nodeBuilder(),
						context.getQuerySource() == null ? getQuerySource() : context.getQuerySource(),
						copyParameters( context ),
						copyCteStatements( context ),
						getTarget().copy( context ),
						copyInsertionTargetPaths( context ),
						getConflictClause() == null ? null : getConflictClause().copy( context ),
						valuesList
				)
		);
	}

	public SqmInsertValuesStatement<T> copyWithoutValues(SqmCopyContext context) {
		return context.registerCopy(
				this,
				new SqmInsertValuesStatement<>(
						nodeBuilder(),
						context.getQuerySource() == null ? getQuerySource() : context.getQuerySource(),
						copyParameters( context ),
						copyCteStatements( context ),
						getTarget().copy( context ),
						copyInsertionTargetPaths( context ),
						getConflictClause() == null ? null : getConflictClause().copy( context ),
						null
				)
		);
	}

	@Override
	public void validate(@Nullable String hql) {
		final List<SqmPath<?>> insertionTargetPaths = getInsertionTargetPaths();
		for ( SqmValues sqmValues : getValuesList() ) {
			verifyInsertTypesMatch( insertionTargetPaths, sqmValues.getExpressions() );
		}
	}

	public List<SqmValues> getValuesList() {
		return valuesList == null
				? Collections.emptyList()
				: Collections.unmodifiableList( valuesList );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitInsertValuesStatement( this );
	}

	@Override
	public <U> Subquery<U> subquery(EntityType<U> type) {
		throw new UnsupportedOperationException( "INSERT query cannot be sub-query" );
	}

	@Override
	public JpaPredicate getRestriction() {
		return null;
	}

	@Override
	public Set<ParameterExpression<?>> getParameters() {
		// At this level, the number of parameters may still be growing as
		// nodes are added to the Criteria - so we re-calculate this every
		// time.
		//
		// for a "finalized" set of parameters, use `#resolveParameters` instead
		assert getQuerySource() == SqmQuerySource.CRITERIA;
		return getSqmParameters().stream()
				.filter( parameterExpression -> !( parameterExpression instanceof ValueBindJpaCriteriaParameter ) )
				.collect( Collectors.toSet() );
	}

	@Override
	public SqmInsertValuesStatement<T> setInsertionTargetPaths(Path<?>... insertionTargetPaths) {
		super.setInsertionTargetPaths( insertionTargetPaths );
		return this;
	}

	@Override
	public SqmInsertValuesStatement<T> setInsertionTargetPaths(List<? extends Path<?>> insertionTargetPaths) {
		super.setInsertionTargetPaths( insertionTargetPaths );
		return this;
	}

	@Override
	public SqmInsertValuesStatement<T> values(JpaValues... values) {
		return values( Arrays.asList( values ) );
	}

	@Override
	public SqmInsertValuesStatement<T> values(List<? extends JpaValues> values) {
		//noinspection unchecked
		this.valuesList = (List<SqmValues>) values;
		return this;
	}

	@Override
	public SqmInsertValuesStatement<T> onConflict(JpaConflictClause<T> conflictClause) {
		super.onConflict( conflictClause );
		return this;
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		assert valuesList != null;
		super.appendHqlString( hql );
		hql.append( " values (" );
		appendValues( valuesList.get( 0 ), hql );
		for ( int i = 1; i < valuesList.size(); i++ ) {
			hql.append( ", " );
			appendValues( valuesList.get( i ), hql );
		}
		hql.append( ')' );
		final SqmConflictClause conflictClause = getConflictClause();
		if ( conflictClause != null ) {
			conflictClause.appendHqlString( hql );
		}
	}

	private static void appendValues(SqmValues sqmValues, StringBuilder sb) {
		final List<SqmExpression<?>> expressions = sqmValues.getExpressions();
		sb.append( '(' );
		expressions.get( 0 ).appendHqlString( sb );
		for ( int i = 1; i < expressions.size(); i++ ) {
			sb.append( ", " );
			expressions.get( i ).appendHqlString( sb );
		}
		sb.append( ')' );
	}
}
