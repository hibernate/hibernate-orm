/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmCreationHelper;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmAttributeJoin<O,T>
		extends AbstractSqmJoin<O,T>
		implements SqmAttributeJoin<O,T> {
	private static final Logger log = Logger.getLogger( AbstractSqmAttributeJoin.class );

	private final SqmFrom<?,O> lhs;
	private final boolean fetched;

	private SqmPredicate onClausePredicate;

	public AbstractSqmAttributeJoin(
			SqmFrom<?,O> lhs,
			Joinable<O,T> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super(
				SqmCreationHelper.buildSubNavigablePath( lhs, joinedNavigable, alias ),
				joinedNavigable,
				lhs,
				alias,
				joinType,
				nodeBuilder
		);
		this.lhs = lhs;
		this.fetched = fetched;
	}

	@Override
	public SqmFrom<?,O> getLhs() {
		return lhs;
	}

	@Override
	public Joinable<O,T> getReferencedNavigable() {
		return (Joinable<O,T>) super.getReferencedNavigable();
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	public boolean isFetched() {
		return fetched;
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return onClausePredicate;
	}

	public void setJoinPredicate(SqmPredicate predicate) {
		log.tracef(
				"Setting join predicate [%s] (was [%s])",
				predicate.toString(),
				this.onClausePredicate == null ? "<null>" : this.onClausePredicate.toString()
		);

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
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedAttributeJoin( this );
	}

	@Override
	public void prepareForSubNavigableReference(
			Navigable subNavigable,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		// nothing to prepare
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public PersistentAttribute<? super O, ?> getAttribute() {
		return getReferencedNavigable();
	}

	@Override
	public SqmAttributeJoin<O, T> on(JpaExpression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public SqmAttributeJoin<O, T> on(Expression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public SqmAttributeJoin<O, T> on(JpaPredicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	public SqmAttributeJoin<O, T> on(Predicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	public Predicate getOn() {
		return getJoinPredicate();
	}

	@Override
	public SqmFrom<?, O> getParent() {
		return getLhs();
	}

	@Override
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}
}
