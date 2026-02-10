/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.insert;

import org.hibernate.query.criteria.JpaConflictUpdateAction;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmNode;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

import static org.hibernate.query.sqm.internal.TypecheckUtil.assertAssignable;

/**
 * @since 6.5
 */
public class SqmConflictUpdateAction<T> implements SqmNode, JpaConflictUpdateAction<T> {

	private final SqmInsertStatement<T> insertStatement;
	private final SqmSetClause setClause;
	private @Nullable SqmWhereClause whereClause;

	public SqmConflictUpdateAction(SqmInsertStatement<T> insertStatement) {
		this.insertStatement = insertStatement;
		this.setClause = new SqmSetClause();
	}

	private SqmConflictUpdateAction(
			SqmInsertStatement<T> insertStatement,
			SqmSetClause setClause,
			@Nullable SqmWhereClause whereClause) {
		this.insertStatement = insertStatement;
		this.setClause = setClause;
		this.whereClause = whereClause;
	}

	@Override
	public <Y, X extends Y> SqmConflictUpdateAction<T> set(SingularAttribute<? super T, Y> attribute, @Nullable X value) {
		applyAssignment( getTarget().get( attribute ), (SqmExpression<? extends Y>) nodeBuilder().value( value ) );
		return this;
	}

	@Override
	public <Y> SqmConflictUpdateAction<T> set(SingularAttribute<? super T, Y> attribute, Expression<? extends Y> value) {
		applyAssignment( getTarget().get( attribute ), (SqmExpression<? extends Y>) value );
		return this;
	}

	@Override
	public <Y, X extends Y> SqmConflictUpdateAction<T> set(Path<Y> attribute, @Nullable X value) {
		applyAssignment( (SqmPath<Y>) attribute, (SqmExpression<? extends Y>) nodeBuilder().value( value ) );
		return this;
	}

	@Override
	public <Y> SqmConflictUpdateAction<T> set(Path<Y> attribute, Expression<? extends Y> value) {
		applyAssignment( (SqmPath<Y>) attribute, (SqmExpression<? extends Y>) value );
		return this;
	}

	@Override
	public SqmConflictUpdateAction<T> set(String attributeName, @Nullable Object value) {
		final SqmPath sqmPath = getTarget().get(attributeName);
		final SqmExpression expression;
		if ( value instanceof SqmExpression ) {
			expression = (SqmExpression) value;
		}
		else {
			expression = (SqmExpression) nodeBuilder().value( value );
		}
		assertAssignable( null, sqmPath, expression, nodeBuilder() );
		applyAssignment( sqmPath, expression );
		return this;
	}

	public void addAssignment(SqmAssignment<?> assignment) {
		setClause.addAssignment( assignment );
	}

	private <Y> void applyAssignment(SqmPath<Y> targetPath, SqmExpression<? extends Y> value) {
		setClause.addAssignment( new SqmAssignment<>( targetPath, value ) );
	}

	@Override
	public SqmConflictUpdateAction<T> where(@Nullable Expression<Boolean> restriction) {
		initAndGetWhereClause().setPredicate( (SqmPredicate) restriction );
		return this;
	}

	@Override
	public SqmConflictUpdateAction<T> where(Predicate @Nullable... restrictions) {
		final SqmWhereClause whereClause = initAndGetWhereClause();
		// Clear the current predicate if one is present
		whereClause.setPredicate( null );
		if ( restrictions != null ) {
			for ( Predicate restriction : restrictions ) {
				whereClause.applyPredicate( (SqmPredicate) restriction );
			}
		}
		return this;
	}

	@Override
	public @Nullable SqmPredicate getRestriction() {
		return whereClause == null ? null : whereClause.getPredicate();
	}

	protected SqmWhereClause initAndGetWhereClause() {
		if ( whereClause == null ) {
			whereClause = new SqmWhereClause( nodeBuilder() );
		}
		return whereClause;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return insertStatement.nodeBuilder();
	}

	@Override
	public SqmConflictUpdateAction<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy(
				this,
				new SqmConflictUpdateAction<>(
						insertStatement.copy( context ),
						setClause.copy( context ),
						whereClause == null ? null : whereClause.copy( context )
				)
		);
	}

	public SqmSetClause getSetClause() {
		return setClause;
	}

	public @Nullable SqmWhereClause getWhereClause() {
		return whereClause;
	}

	private SqmRoot<T> getTarget() {
		return insertStatement.getTarget();
	}

	public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
		sb.append( " do update" );
		setClause.appendHqlString( sb, context );

		final SqmPredicate predicate = whereClause == null ? null : whereClause.getPredicate();
		if ( predicate != null ) {
			sb.append( " where " );
			predicate.appendHqlString( sb, context );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmConflictUpdateAction<?> that
			&& setClause.equals( that.getSetClause() )
			&& Objects.equals( whereClause, that.getWhereClause() );
	}

	@Override
	public int hashCode() {
		int result = setClause.hashCode();
		result = 31 * result + Objects.hashCode( whereClause );
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmConflictUpdateAction<?> that
				&& setClause.isCompatible( that.getSetClause() )
				&& SqmCacheable.areCompatible( whereClause, that.getWhereClause() );
	}

	@Override
	public int cacheHashCode() {
		int result = setClause.cacheHashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( whereClause );
		return result;
	}
}
