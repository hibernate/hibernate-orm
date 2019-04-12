/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public abstract class AbstractSqmSelectQuery<T>
		extends AbstractSqmNode
		implements SqmSelectQuery<T> {

	private SqmQuerySpec<T> sqmQuerySpec;
	private Class resultType;


	public AbstractSqmSelectQuery(Class<T> resultType, NodeBuilder builder) {
		super( builder );
		this.sqmQuerySpec = new SqmQuerySpec( builder );
		this.resultType = resultType;
	}


	public AbstractSqmSelectQuery(SqmQuerySpec<T> sqmQuerySpec, NodeBuilder builder) {
		super( builder );
		this.sqmQuerySpec = sqmQuerySpec;
		this.resultType = sqmQuerySpec.getSelectClause().getJavaType();
	}

	@Override
	public Class getResultType() {
		return resultType;
	}

	protected void setResultType(Class resultType) {
		this.resultType = resultType;
	}

	@Override
	public SqmQuerySpec<T> getQuerySpec() {
		return sqmQuerySpec;
	}

	public void setQuerySpec(SqmQuerySpec<T> sqmQuerySpec) {
		this.sqmQuerySpec = sqmQuerySpec;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Root<?>> getRoots() {
		return (Set) sqmQuerySpec.getRoots();
	}

	@Override
	public <X> SqmRoot<X> from(Class<X> entityClass) {
		return addRoot(
				new SqmRoot<>(
						nodeBuilder().getDomainModel().getEntityDescriptor( entityClass ),
						null,
						nodeBuilder()
				)
		);

	}

	private <X> SqmRoot<X> addRoot(SqmRoot<X> root) {
		sqmQuerySpec.addRoot( root );
		return root;
	}

	@Override
	public <X> SqmRoot<X> from(EntityType<X> entityType) {
		return addRoot( new SqmRoot<>( (EntityTypeDescriptor<X>) entityType, null, nodeBuilder() ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selection

	@Override
	public boolean isDistinct() {
		return sqmQuerySpec.isDistinct();
	}

	@Override
	public SqmSelectQuery<T> distinct(boolean distinct) {
		sqmQuerySpec.setDistinct( distinct );
		return this;
	}

	@Override
	public JpaSelection<T> getSelection() {
		return sqmQuerySpec.getSelection();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Restriction

	@Override
	public SqmPredicate getRestriction() {
		return sqmQuerySpec.getRestriction();
	}

	@Override
	public SqmSelectQuery<T> where(Expression<Boolean> restriction) {
		sqmQuerySpec.setRestriction( restriction );
		return this;
	}

	@Override
	public SqmSelectQuery<T> where(Predicate... restrictions) {
		sqmQuerySpec.setRestriction( restrictions );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping

	@Override
	@SuppressWarnings("unchecked")
	public List<Expression<?>> getGroupList() {
		return (List) sqmQuerySpec.getGroupingExpressions();
	}

	@Override
	public SqmSelectQuery<T> groupBy(Expression<?>... expressions) {
		return groupBy( Arrays.asList( expressions ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSelectQuery<T> groupBy(List<Expression<?>> grouping) {
		getQuerySpec().setGroupingExpressions( (List) grouping );
		return this;
	}

	@Override
	public SqmPredicate getGroupRestriction() {
		return sqmQuerySpec.getGroupRestriction();
	}

	@Override
	public SqmSelectQuery<T> having(Expression<Boolean> booleanExpression) {
		sqmQuerySpec.setGroupRestriction( nodeBuilder().wrap( booleanExpression ) );
		return this;
	}

	@Override
	public SqmSelectQuery<T> having(Predicate... predicates) {
		sqmQuerySpec.setGroupRestriction( nodeBuilder().wrap( predicates ) );
		return this;
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
