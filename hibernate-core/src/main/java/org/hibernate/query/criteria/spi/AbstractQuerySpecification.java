/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.EntityType;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaQuerySpecification;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractQuerySpecification<T>
		extends AbstractNode
		implements JpaQuerySpecification<T> {

	private final Class<T> resultType;

	private boolean distinct;
	private SelectionImplementor<T> selection;

	private final Set<RootImpl<?>> roots = new LinkedHashSet<>();

	private PredicateImplementor restriction;

	private List<SortSpecification> sortSpecifications;

	private List<ExpressionImplementor<?>> grouping;
	private PredicateImplementor having;

	private ExpressionImplementor limit;
	private ExpressionImplementor offset;


	public AbstractQuerySpecification(Class<T> resultType, CriteriaNodeBuilder builder) {
		super( builder );
		this.resultType = resultType;
	}

	@Override
	public Class<T> getResultType() {
		return resultType;
	}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Roots

	@Override
	public Set<? extends RootImplementor> getJpaRoots() {
		return roots;
	}

	@Override
	public <X> RootImpl<X> from(Class<X> entityClass) {
		final RootImpl<X> root = new RootImpl<>(
				nodeBuilder().getSessionFactory().getMetamodel().getEntityDescriptor( entityClass ),
				nodeBuilder()
		);
		roots.add( root );
		return root;

	}

	@Override
	public <X> JpaRoot<X> from(EntityType<X> entityType) {
		final RootImpl<X> root = new RootImpl<>( (EntityTypeDescriptor<X>) entityType, nodeBuilder() );
		roots.add( root );
		return root;
	}

	@Override
	public <X> JpaRoot<X> from(EntityDomainType<X> entityType) {
		final RootImpl<X> root = new RootImpl<>( (EntityTypeDescriptor<X>) entityType, nodeBuilder() );
		roots.add( root );
		return root;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selection

	@Override
	public boolean isDistinct() {
		return distinct;
	}

	@Override
	public JpaQuerySpecification<T> distinct(boolean distinct) {
		this.distinct = distinct;
		return this;
	}

	@Override
	public SelectionImplementor<T> getSelection() {
		return selection;
	}

	@Override
	public JpaQuerySpecification<T> setSelection(JpaSelection<T> selection) {
		this.selection = (SelectionImplementor<T>) selection;
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Restriction

	@Override
	public PredicateImplementor getRestriction() {
		return restriction;
	}

	@Override
	public JpaQuerySpecification<T> setWhere(JpaPredicate restriction) {
		this.restriction = (PredicateImplementor) restriction;
		return this;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordering

	@Override
	public List<SortSpecification> getSortSpecifications() {
		return sortSpecifications == null ? Collections.emptyList() : sortSpecifications;
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaQuerySpecification<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications) {
		this.sortSpecifications = (List) sortSpecifications;
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping


	@Override
	public List<? extends JpaExpression<?>> getGroupByList() {
		return grouping;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Expression<?>> getGroupList() {
		return (List) grouping;
	}

	@Override
	public JpaQuerySpecification<T> setGroupBy(List<? extends JpaExpression<?>> grouping) {
		return this;
	}

	@Override
	public JpaQuerySpecification<T> groupBy(Expression<?>... expressions) {
		return groupBy( Arrays.asList( expressions ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaQuerySpecification<T> groupBy(List<Expression<?>> grouping) {
		setGroupBy( (List) grouping );
		return this;
	}

	@Override
	public PredicateImplementor getGroupRestriction() {
		return having;
	}

	@Override
	public JpaQuerySpecification<T> setGroupRestriction(JpaPredicate predicate) {
		this.having = (PredicateImplementor) predicate;
		return this;
	}

	@Override
	public JpaQuerySpecification<T> having(Expression<Boolean> booleanExpression) {
		return setGroupRestriction( nodeBuilder().wrap( booleanExpression ) );
	}

	@Override
	public JpaQuerySpecification<T> having(Predicate... predicates) {
		return setGroupRestriction( nodeBuilder().wrap( predicates ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit

	@Override
	@SuppressWarnings("unchecked")
	public <X> ExpressionImplementor<X> getLimit() {
		return limit;
	}

	@Override
	public JpaQuerySpecification<T> setLimit(JpaExpression<?> limit) {
		this.limit = (ExpressionImplementor) limit;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> JpaExpression<X> getOffset() {
		return offset;
	}

	@Override
	public JpaQuerySpecification<T> setOffset(JpaExpression offset) {
		this.offset = (ExpressionImplementor) offset;
		return this;
	}
}
