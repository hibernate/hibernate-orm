/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

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
public abstract class AbstractSqmQualifiedJoin<O, T> extends AbstractSqmJoin<O,T> implements SqmQualifiedJoin<O, T>, JpaJoinedFrom<O, T> {

	private SqmPredicate onClausePredicate;

	public AbstractSqmQualifiedJoin(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedNavigable,
			SqmFrom<?, ?> lhs, String alias, SqmJoinType joinType, NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, alias, joinType, nodeBuilder );
	}

	protected void copyTo(AbstractSqmQualifiedJoin<O, T> target, SqmCopyContext context) {
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
	public JpaJoinedFrom<O, T> on(JpaExpression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public JpaJoinedFrom<O, T> on(Expression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public JpaJoinedFrom<O, T> on(JpaPredicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	public JpaJoinedFrom<O, T> on(Predicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}
}
