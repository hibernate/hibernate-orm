/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.insert;

import org.hibernate.query.criteria.JpaConflictUpdateAction;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmNode;
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
	public <Y, X extends Y> SqmConflictUpdateAction<T> set(SingularAttribute<? super T, Y> attribute, X value) {
		applyAssignment( getTarget().get( attribute ), (SqmExpression<? extends Y>) nodeBuilder().value( value ) );
		return this;
	}

	@Override
	public <Y> SqmConflictUpdateAction<T> set(SingularAttribute<? super T, Y> attribute, Expression<? extends Y> value) {
		applyAssignment( getTarget().get( attribute ), (SqmExpression<? extends Y>) value );
		return this;
	}

	@Override
	public <Y, X extends Y> SqmConflictUpdateAction<T> set(Path<Y> attribute, X value) {
		applyAssignment( (SqmPath<Y>) attribute, (SqmExpression<? extends Y>) nodeBuilder().value( value ) );
		return this;
	}

	@Override
	public <Y> SqmConflictUpdateAction<T> set(Path<Y> attribute, Expression<? extends Y> value) {
		applyAssignment( (SqmPath<Y>) attribute, (SqmExpression<? extends Y>) value );
		return this;
	}

	@Override
	public SqmConflictUpdateAction<T> set(String attributeName, Object value) {
		final SqmPath sqmPath = getTarget().get(attributeName);
		final SqmExpression expression;
		if ( value instanceof SqmExpression ) {
			expression = (SqmExpression) value;
		}
		else {
			expression = (SqmExpression) nodeBuilder().value( value );
		}
		assertAssignable( null, sqmPath, expression, nodeBuilder().getSessionFactory() );
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
	public SqmConflictUpdateAction<T> where(Expression<Boolean> restriction) {
		initAndGetWhereClause().setPredicate( (SqmPredicate) restriction );
		return this;
	}

	@Override
	public SqmConflictUpdateAction<T> where(Predicate... restrictions) {
		final SqmWhereClause whereClause = initAndGetWhereClause();
		// Clear the current predicate if one is present
		whereClause.setPredicate(null);
		for ( Predicate restriction : restrictions ) {
			whereClause.applyPredicate( (SqmPredicate) restriction );
		}
		return this;
	}

	@Override
	public SqmPredicate getRestriction() {
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
		final SqmConflictUpdateAction<T> existing = context.getCopy( this );
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

	public SqmWhereClause getWhereClause() {
		return whereClause;
	}

	private SqmRoot<T> getTarget() {
		return insertStatement.getTarget();
	}

	public void appendHqlString(StringBuilder sb) {
		sb.append( " do update" );
		setClause.appendHqlString( sb );

		if ( whereClause != null && whereClause.getPredicate() != null ) {
			sb.append( " where " );
			whereClause.getPredicate().appendHqlString( sb );
		}
	}
}
