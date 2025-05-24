/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.delete;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.query.criteria.JpaCriteriaDelete;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.AbstractSqmRestrictedDmlStatement;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

/**
 * @author Steve Ebersole
 */
public class SqmDeleteStatement<T>
		extends AbstractSqmRestrictedDmlStatement<T>
		implements SqmDeleteOrUpdateStatement<T>, JpaCriteriaDelete<T> {

	public SqmDeleteStatement(NodeBuilder nodeBuilder) {
		super( SqmQuerySource.HQL, nodeBuilder );
	}

	public SqmDeleteStatement(Class<T> targetEntity, NodeBuilder nodeBuilder) {
		super(
				new SqmRoot<>(
						nodeBuilder.getDomainModel().entity( targetEntity ),
						"_0",
						!nodeBuilder.isJpaQueryComplianceEnabled(),
						nodeBuilder
				),
				SqmQuerySource.CRITERIA,
				nodeBuilder
		);
	}

	public SqmDeleteStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmRoot<T> target) {
		super( builder, querySource, parameters, cteStatements, target );
	}

	@Override
	public SqmDeleteStatement<T> copy(SqmCopyContext context) {
		final SqmDeleteStatement<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmDeleteStatement<T> statement = context.registerCopy(
				this,
				new SqmDeleteStatement<>(
						nodeBuilder(),
						context.getQuerySource() == null ? getQuerySource() : context.getQuerySource(),
						copyParameters( context ),
						copyCteStatements( context ),
						getTarget().copy( context )
				)
		);
		statement.setWhereClause( copyWhereClause( context ) );
		return statement;
	}

	@Override
	public void validate(@Nullable String hql) {
		// No-op
	}

	@Override
	public SqmDeleteStatement<T> where(Expression<Boolean> restriction) {
		setWhere( restriction );
		return this;
	}

	@Override
	public SqmDeleteStatement<T> where(Predicate... restrictions) {
		setWhere( restrictions );
		return this;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitDeleteStatement( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		appendHqlCteString( hql, context );
		hql.append( "delete from " );
		final SqmRoot<T> root = getTarget();
		hql.append( root.getEntityName() );
		hql.append( ' ' ).append( root.resolveAlias( context ) );
		SqmFromClause.appendJoins( root, hql, context );
		SqmFromClause.appendTreatJoins( root, hql, context );
		super.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(Object node) {
		return node instanceof SqmDeleteStatement<?> that
			&& super.equals( node )
			&& Objects.equals( this.getTarget(), that.getTarget() )
			&& Objects.equals( this.getWhereClause(), that.getWhereClause() )
			&& Objects.equals( this.getCteStatements(), that.getCteStatements() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), getTarget(), getWhereClause(), getCteStatements() );
	}

	@Override
	public <U> Subquery<U> subquery(EntityType<U> type) {
		throw new UnsupportedOperationException( "DELETE query cannot be sub-query" );
	}
}
