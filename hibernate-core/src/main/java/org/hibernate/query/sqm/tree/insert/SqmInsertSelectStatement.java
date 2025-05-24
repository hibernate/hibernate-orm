/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.insert;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaConflictClause;
import org.hibernate.query.criteria.JpaCriteriaInsertSelect;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.ValueBindJpaCriteriaParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

/**
 * @author Steve Ebersole
 */
@Incubating
public class SqmInsertSelectStatement<T> extends AbstractSqmInsertStatement<T> implements JpaCriteriaInsertSelect<T> {
	private SqmQueryPart<?> selectQueryPart;

	public SqmInsertSelectStatement(SqmRoot<T> targetRoot, NodeBuilder nodeBuilder) {
		super( targetRoot, SqmQuerySource.HQL, nodeBuilder );
		this.selectQueryPart = new SqmQuerySpec<>( nodeBuilder );
	}

	public SqmInsertSelectStatement(Class<T> targetEntity, NodeBuilder nodeBuilder) {
		super(
				new SqmRoot<>(
						nodeBuilder.getDomainModel().entity( targetEntity ),
						"_0",
						false,
						nodeBuilder
				),
				SqmQuerySource.CRITERIA,
				nodeBuilder
		);
		this.selectQueryPart = new SqmQuerySpec<>( nodeBuilder );
	}

	private SqmInsertSelectStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmRoot<T> target,
			List<SqmPath<?>> insertionTargetPaths,
			SqmConflictClause<T> conflictClause,
			SqmQueryPart<?> selectQueryPart) {
		super( builder, querySource, parameters, cteStatements, target, insertionTargetPaths, conflictClause );
		this.selectQueryPart = selectQueryPart;
	}

	@Override
	public SqmInsertSelectStatement<T> copy(SqmCopyContext context) {
		final SqmInsertSelectStatement<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmInsertSelectStatement<T> sqmInsertSelectStatementCopy = new SqmInsertSelectStatement<>(
				nodeBuilder(),
				context.getQuerySource() == null ? getQuerySource() : context.getQuerySource(),
				copyParameters( context ),
				copyCteStatements( context ),
				getTarget().copy( context ),
				null,
				null,
				selectQueryPart.copy( context )
		);

		context.registerCopy( this, sqmInsertSelectStatementCopy );

		sqmInsertSelectStatementCopy.setInsertionTargetPaths( copyInsertionTargetPaths( context ) );

		if ( getConflictClause() != null ) {
			sqmInsertSelectStatementCopy.setConflictClause( getConflictClause().copy( context ) );
		}

		return sqmInsertSelectStatementCopy;
	}

	@Override
	public void validate(@Nullable String hql) {
		final List<SqmPath<?>> insertionTargetPaths = getInsertionTargetPaths();
		final List<SqmSelectableNode<?>> selections = getSelectQueryPart()
				.getFirstQuerySpec()
				.getSelectClause()
				.getSelectionItems();
		verifyInsertTypesMatch( insertionTargetPaths, selections );
		getSelectQueryPart().validateQueryStructureAndFetchOwners();
	}

	@Override
	public SqmInsertSelectStatement<T> select(CriteriaQuery<Tuple> criteriaQuery) {
		final SqmSelectStatement<Tuple> selectStatement = (SqmSelectStatement<Tuple>) criteriaQuery;
		putAllCtes( selectStatement );
		setSelectQueryPart( selectStatement.getQueryPart() );
		return this;
	}

	public SqmQueryPart<?> getSelectQueryPart() {
		return selectQueryPart;
	}

	public void setSelectQueryPart(SqmQueryPart<?> selectQueryPart) {
		this.selectQueryPart = selectQueryPart;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitInsertSelectStatement( this );
	}

	@Override
	public <U> Subquery<U> subquery(EntityType<U> type) {
		throw new UnsupportedOperationException( "INSERT cannot be basis for subquery" );
	}

	@Override
	public JpaPredicate getRestriction() {
		// insert has no predicate
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
	public SqmInsertSelectStatement<T> setInsertionTargetPaths(Path<?>... insertionTargetPaths) {
		super.setInsertionTargetPaths( insertionTargetPaths );
		return this;
	}

	@Override
	public SqmInsertSelectStatement<T> setInsertionTargetPaths(List<? extends Path<?>> insertionTargetPaths) {
		super.setInsertionTargetPaths( insertionTargetPaths );
		return this;
	}

	@Override
	public SqmInsertSelectStatement<T> onConflict(JpaConflictClause<T> conflictClause) {
		super.onConflict( conflictClause );
		return this;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		super.appendHqlString( hql, context );
		hql.append( ' ' );
		selectQueryPart.appendHqlString( hql, context );
		final SqmConflictClause<?> conflictClause = getConflictClause();
		if ( conflictClause != null ) {
			conflictClause.appendHqlString( hql, context );
		}
	}

	@Override
	public boolean equals(Object object) {
		if ( !(object instanceof SqmInsertSelectStatement<?> that) ) {
			return false;
		}
		return Objects.equals( selectQueryPart, that.selectQueryPart )
			&& Objects.equals( this.getTarget(), that.getTarget() )
			&& Objects.equals( this.getInsertionTargetPaths(), that.getInsertionTargetPaths() )
			&& Objects.equals( this.getConflictClause(), that.getConflictClause() )
			&& Objects.equals( this.getCteStatements(), that.getCteStatements() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( selectQueryPart, getTarget(), getInsertionTargetPaths(), getConflictClause(), getCteStatements() );
	}
}
