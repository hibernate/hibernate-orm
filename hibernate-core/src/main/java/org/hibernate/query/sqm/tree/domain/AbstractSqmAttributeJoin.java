/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.JoinType;

/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmAttributeJoin<L, R>
		extends AbstractSqmQualifiedJoin<L, R>
		implements SqmAttributeJoin<L, R> {

	private boolean fetched;

	public AbstractSqmAttributeJoin(
			SqmFrom<?, L> lhs,
			SqmJoinable joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		this(
				lhs,
				joinedNavigable.createNavigablePath( lhs, alias ),
				joinedNavigable,
				alias == SqmCreationHelper.IMPLICIT_ALIAS ? null : alias,
				joinType,
				fetched,
				nodeBuilder
		);
	}

	protected AbstractSqmAttributeJoin(
			SqmFrom<?, L> lhs,
			NavigablePath navigablePath,
			SqmJoinable joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				navigablePath,
				(SqmPathSource<R>) joinedNavigable,
				lhs,
				alias,
				joinType,
				nodeBuilder
		);
		this.fetched = fetched;
		validateFetchAlias( alias );
	}

	@Override
	public SqmFrom<?, L> getLhs() {
		//noinspection unchecked
		return (SqmFrom<?, L>) super.getLhs();
	}

	@Override
	public JavaType<R> getNodeJavaType() {
		return getJavaTypeDescriptor();
	}

	@Override
	public boolean isFetched() {
		return fetched;
	}

	@Override
	public JpaSelection<T> alias(String name) {
		validateFetchAlias( name );
		return super.alias( name );
	}

	@Override
	public void clearFetched() {
		fetched = false;
	}

	private void validateFetchAlias(String alias) {
		if ( fetched && alias != null && nodeBuilder().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not permit specifying an alias for fetch joins."
			);
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedAttributeJoin( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public PersistentAttribute<? super L, ?> getAttribute() {
		//noinspection unchecked
		return (PersistentAttribute<? super L, ?>) getReferencedPathSource();
	}

	@Override
	public SqmAttributeJoin<L, R> on(JpaExpression<Boolean> restriction) {
		return (SqmAttributeJoin<L, R>) super.on( restriction );
	}

	@Override
	public SqmAttributeJoin<L, R> on(JpaPredicate... restrictions) {
		return (SqmAttributeJoin<L, R>) super.on( restrictions );
	}

	@Override
	public SqmFrom<?, L> getParent() {
		return getLhs();
	}

	@Override
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public <S extends R> SqmAttributeJoin<L, S> treatAs(Class<S> treatTarget) {
		return (SqmAttributeJoin<L, S>) super.treatAs( treatTarget );
	}

	@Override
	public <S extends R> SqmAttributeJoin<L, S> treatAs(EntityDomainType<S> treatTarget) {
		return (SqmAttributeJoin<L, S>) super.treatAs( treatTarget );
	}
}
