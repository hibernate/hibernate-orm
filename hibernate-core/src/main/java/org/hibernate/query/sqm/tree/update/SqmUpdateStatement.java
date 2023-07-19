/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.update;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.criteria.JpaCriteriaUpdate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmRestrictedDmlStatement;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public class SqmUpdateStatement<T>
		extends AbstractSqmRestrictedDmlStatement<T>
		implements SqmDeleteOrUpdateStatement<T>, JpaCriteriaUpdate<T> {
	private boolean versioned;
	private SqmSetClause setClause;

	public SqmUpdateStatement(SqmRoot<T> target, NodeBuilder nodeBuilder) {
		this( target, SqmQuerySource.HQL, nodeBuilder );
	}

	public SqmUpdateStatement(SqmRoot<T> target, SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( target, querySource, nodeBuilder );
	}

	public SqmUpdateStatement(Class<T> targetEntity, SqmCriteriaNodeBuilder nodeBuilder) {
		this(
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

	public SqmUpdateStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmRoot<T> target) {
		super( builder, querySource, parameters, cteStatements, target );
	}

	@Override
	public SqmUpdateStatement<T> copy(SqmCopyContext context) {
		final SqmUpdateStatement<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmUpdateStatement<T> statement = context.registerCopy(
				this,
				new SqmUpdateStatement<>(
						nodeBuilder(),
						getQuerySource(),
						copyParameters( context ),
						copyCteStatements( context ),
						getTarget().copy( context )
				)
		);
		statement.setWhereClause( copyWhereClause( context ) );
		statement.versioned = versioned;
		if ( setClause != null ) {
			statement.setClause = setClause.copy( context );
		}
		return statement;
	}

	public SqmSetClause getSetClause() {
		return setClause;
	}

	public void setSetClause(SqmSetClause setClause) {
		this.setClause = setClause;
	}

	@Override
	public <Y, X extends Y> SqmUpdateStatement<T> set(SingularAttribute<? super T, Y> attribute, X value) {
		applyAssignment( getTarget().get( attribute ), (SqmExpression<? extends Y>) nodeBuilder().value( value ) );
		return this;
	}

	@Override
	public <Y> SqmUpdateStatement<T> set(SingularAttribute<? super T, Y> attribute, Expression<? extends Y> value) {
		applyAssignment( getTarget().get( attribute ), (SqmExpression<? extends Y>) value );
		return this;
	}

	@Override
	public <Y, X extends Y> SqmUpdateStatement<T> set(Path<Y> attribute, X value) {
		applyAssignment( (SqmPath<Y>) attribute, (SqmExpression<? extends Y>) nodeBuilder().value( value ) );
		return this;
	}

	@Override
	public <Y> SqmUpdateStatement<T> set(Path<Y> attribute, Expression<? extends Y> value) {
		applyAssignment( (SqmPath<Y>) attribute, (SqmExpression<? extends Y>) value );
		return this;
	}

	@Override
	public SqmUpdateStatement<T> set(String attributeName, Object value) {
		//noinspection unchecked
		final SqmPath<Object> sqmPath = (SqmPath<Object>) getTarget().get( attributeName );
		applyAssignment( sqmPath, (SqmExpression<?>) nodeBuilder().value( value ) );
		return this;
	}

	@Override
	public boolean isVersioned() {
		return versioned;
	}

	@Override
	public SqmUpdateStatement<T> versioned() {
		this.versioned = true;
		return this;
	}

	@Override
	public SqmUpdateStatement<T> versioned(boolean versioned) {
		this.versioned = versioned;
		return this;
	}

	@Override
	public SqmUpdateStatement<T> where(Expression<Boolean> restriction) {
		setWhere( restriction );
		return this;
	}

	@Override
	public SqmUpdateStatement<T> where(Predicate... restrictions) {
		setWhere( restrictions );
		return this;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitUpdateStatement( this );
	}

	public <Y> void applyAssignment(SqmPath<Y> targetPath, SqmExpression<? extends Y> value) {
		if ( setClause == null ) {
			setClause = new SqmSetClause();
		}
		setClause.addAssignment( new SqmAssignment<>( targetPath, value ) );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		appendHqlCteString( sb );
		sb.append( "update " );
		if ( versioned ) {
			sb.append( "versioned " );
		}
		sb.append( getTarget().getEntityName() );
		sb.append( ' ' ).append( getTarget().resolveAlias() );
		sb.append( " set " );
		final List<SqmAssignment<?>> assignments = setClause.getAssignments();
		appendAssignment( assignments.get( 0 ), sb );
		for ( int i = 1; i < assignments.size(); i++ ) {
			sb.append( ", " );
			appendAssignment( assignments.get( i ), sb );
		}

		super.appendHqlString( sb );
	}

	private static void appendAssignment(SqmAssignment<?> sqmAssignment, StringBuilder sb) {
		sqmAssignment.getTargetPath().appendHqlString( sb );
		sb.append( " = " );
		sqmAssignment.getValue().appendHqlString( sb );
	}
}
