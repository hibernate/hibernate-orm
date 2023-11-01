/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaJoinedFrom;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmQualifiedJoin<L, R> extends AbstractSqmJoin<L,R> implements SqmQualifiedJoin<L,R>, JpaJoinedFrom<L, R> {

	private SqmPredicate onClausePredicate;

	public AbstractSqmQualifiedJoin(
			NavigablePath navigablePath,
			SqmPathSource<R> referencedNavigable,
			SqmFrom<?, ?> lhs, String alias, SqmJoinType joinType, NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, alias, joinType, nodeBuilder );
	}

	protected void copyTo(AbstractSqmQualifiedJoin<L, R> target, SqmCopyContext context) {
		super.copyTo( target, context );
		target.onClausePredicate = onClausePredicate == null ? null : onClausePredicate.copy( context );
	}

	@Override
	public JpaPredicate getOn() {
		return onClausePredicate;
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return onClausePredicate;
	}



	@Override
	public void setJoinPredicate(SqmPredicate predicate) {
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Setting join predicate [%s] (was [%s])",
					predicate.toString(),
					this.onClausePredicate == null ? "<null>" : this.onClausePredicate.toString()
			);
		}

		this.onClausePredicate = predicate;
	}

	public void applyRestriction(SqmPredicate restriction) {
		if ( this.onClausePredicate == null ) {
			this.onClausePredicate = restriction;
		}
		else {
			this.onClausePredicate = nodeBuilder().and( onClausePredicate, restriction );
		}
	}

	@Override
	public <S extends R> SqmQualifiedJoin<L, S> treatAs(Class<S> treatAsType) {
		return treatAs( treatAsType, null );
	}

	@Override
	public <S extends R> SqmQualifiedJoin<L, S> treatAs(EntityDomainType<S> treatAsType) {
		return treatAs( treatAsType, null );
	}

	@Override
	public JpaJoinedFrom<L, R> on(JpaExpression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public JpaJoinedFrom<L, R> on(Expression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public JpaJoinedFrom<L, R> on(JpaPredicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	public JpaJoinedFrom<L, R> on(Predicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}
}
