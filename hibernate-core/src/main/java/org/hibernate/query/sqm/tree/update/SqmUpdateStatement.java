/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.update;

import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCriteriaUpdate;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;

/**
 * @author Steve Ebersole
 */
public class SqmUpdateStatement<T>
		extends AbstractSqmDmlStatement<T>
		implements SqmDeleteOrUpdateStatement<T>, JpaCriteriaUpdate<T> {
	private boolean versioned;
	private SqmSetClause setClause;
	private SqmWhereClause whereClause;

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
						nodeBuilder
				),
				SqmQuerySource.CRITERIA,
				nodeBuilder
		);
	}

	public SqmSetClause getSetClause() {
		return setClause;
	}

	public void setSetClause(SqmSetClause setClause) {
		this.setClause = setClause;
	}

	@Override
	public SqmWhereClause getWhereClause() {
		return whereClause;
	}

	@Override
	public void applyPredicate(SqmPredicate predicate) {
		if ( predicate == null ) {
			return;
		}

		if ( whereClause == null ) {
			whereClause = new SqmWhereClause( nodeBuilder() );
		}

		whereClause.applyPredicate( predicate );
	}

	public void setWhereClause(SqmWhereClause whereClause) {
		this.whereClause = whereClause;
	}

	@Override
	public Root<T> from(Class<T> entityClass) {
		final EntityDomainType<T> entity = nodeBuilder().getDomainModel().entity( entityClass );
		SqmRoot<T> root = new SqmRoot<>( entity, null, nodeBuilder() );
		setTarget( root );
		return root;
	}

	@Override
	public Root<T> from(EntityType<T> entity) {
		SqmRoot<T> root = new SqmRoot<>( (EntityDomainType<T>) entity, null, nodeBuilder() );
		setTarget( root );
		return root;
	}

	@Override
	public Root<T> getRoot() {
		return getTarget();
	}

	@Override
	public <Y, X extends Y> SqmUpdateStatement<T> set(SingularAttribute<? super T, Y> attribute, X value) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <Y> SqmUpdateStatement<T> set(SingularAttribute<? super T, Y> attribute, Expression<? extends Y> value) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <Y, X extends Y> SqmUpdateStatement<T> set(Path<Y> attribute, X value) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <Y> SqmUpdateStatement<T> set(Path<Y> attribute, Expression<? extends Y> value) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmUpdateStatement<T> set(String attributeName, Object value) {
		throw new NotYetImplementedFor6Exception();
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
		getWhereClause().setPredicate( (SqmPredicate) restriction );
		return this;
	}

	@Override
	public SqmUpdateStatement<T> where(Predicate... restrictions) {
		getWhereClause().setPredicate( null );
		for ( Predicate restriction : restrictions ) {
			getWhereClause().applyPredicate( (SqmPredicate) restriction );
		}
		return this;
	}

	@Override
	public JpaPredicate getRestriction() {
		return whereClause == null ? null : whereClause.getPredicate();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitUpdateStatement( this );
	}

	public void applyAssignment(SqmPath targetPath, SqmExpression value) {
		if ( setClause == null ) {
			setClause = new SqmSetClause();
		}
		setClause.addAssignment( new SqmAssignment( targetPath, value ) );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "update " );
		if ( versioned ) {
			sb.append( "versioned " );
		}
		sb.append( getTarget().getEntityName() );
		if ( getTarget().getExplicitAlias() != null ) {
			sb.append( ' ' ).append( getTarget().getExplicitAlias() );
		}
		sb.append( " set " );
		final List<SqmAssignment> assignments = setClause.getAssignments();
		appendAssignment( assignments.get( 0 ), sb );
		for ( int i = 1; i < assignments.size(); i++ ) {
			sb.append( ", " );
			appendAssignment( assignments.get( i ), sb );
		}

		if ( whereClause != null && whereClause.getPredicate() != null ) {
			sb.append( " where " );
			whereClause.getPredicate().appendHqlString( sb );
		}
	}

	private static void appendAssignment(SqmAssignment sqmAssignment, StringBuilder sb) {
		sqmAssignment.getTargetPath().appendHqlString( sb );
		sb.append( " = " );
		sqmAssignment.getValue().appendHqlString( sb );
	}
}
