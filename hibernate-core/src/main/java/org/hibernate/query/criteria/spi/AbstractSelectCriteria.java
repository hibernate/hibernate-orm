/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSelectCriteria<T, C extends SelectCriteriaImplementor<T>>
		extends AbstractNode
		implements SelectCriteriaImplementor<T> {

	private final QueryStructure<T> queryStructure;
	private final Class<T> resultType;


	public AbstractSelectCriteria(Class<T> resultType, CriteriaNodeBuilder builder) {
		super( builder );
		this.queryStructure = new QueryStructure<>( builder );
		this.resultType = resultType;
	}

	@Override
	public Class<T> getResultType() {
		return resultType;
	}

	@Override
	public QueryStructure<T> getQueryStructure() {
		return queryStructure;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Root<?>> getRoots() {
		return (Set) queryStructure.getRoots();
	}

	@Override
	public <X> RootImplementor<X> from(Class<X> entityClass) {
		return addRoot(
				new RootImpl<>(
						nodeBuilder().getSessionFactory().getMetamodel().getEntityDescriptor( entityClass ),
						nodeBuilder()
				)
		);

	}

	private <X> RootImplementor<X> addRoot(RootImpl<X> root) {
		queryStructure.addRoot( root );
		return root;
	}

	@Override
	public <X> RootImplementor<X> from(EntityType<X> entityType) {
		return addRoot( new RootImpl<>( (EntityTypeDescriptor<X>) entityType, nodeBuilder() ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selection

	@Override
	public boolean isDistinct() {
		return queryStructure.isDistinct();
	}

	@Override
	public C distinct(boolean distinct) {
		queryStructure.setDistinct( distinct );
		//noinspection unchecked
		return (C) this;
	}

	@Override
	public SelectionImplementor<T> getSelection() {
		return queryStructure.getSelection();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Restriction

	@Override
	public PredicateImplementor getRestriction() {
		return queryStructure.getRestriction();
	}

	@Override
	public C where(Expression<Boolean> restriction) {
		queryStructure.setRestriction( restriction );
		//noinspection unchecked
		return (C) this;
	}

	@Override
	public C where(Predicate... restrictions) {
		queryStructure.setRestriction( restrictions );
		//noinspection unchecked
		return (C) this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping

	@Override
	@SuppressWarnings("unchecked")
	public List<Expression<?>> getGroupList() {
		return (List) queryStructure.getGroupingExpressions();
	}

	@Override
	public C groupBy(Expression<?>... expressions) {
		return groupBy( Arrays.asList( expressions ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public C groupBy(List<Expression<?>> grouping) {
		getQueryStructure().setGroupingExpressions( (List) grouping );
		//noinspection unchecked
		return (C) this;
	}

	@Override
	public PredicateImplementor getGroupRestriction() {
		return queryStructure.getGroupRestriction();
	}

	@Override
	public C having(Expression<Boolean> booleanExpression) {
		queryStructure.setGroupRestriction( nodeBuilder().wrap( booleanExpression ) );
		//noinspection unchecked
		return (C) this;
	}

	@Override
	public C having(Predicate... predicates) {
		queryStructure.setGroupRestriction( nodeBuilder().wrap( predicates ) );
		//noinspection unchecked
		return (C) this;
	}

//
//	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	// Limit
//
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public <X> ExpressionImplementor<X> getLimit() {
//		return limit;
//	}
//
//	@Override
//	public C setLimit(JpaExpression<?> limit) {
//		this.limit = (ExpressionImplementor) limit;
//		return this;
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public <X> ExpressionImplementor<X> getOffset() {
//		return offset;
//	}
//
//	@Override
//	public C setOffset(JpaExpression offset) {
//		this.offset = (ExpressionImplementor) offset;
//		return this;
//	}
}
