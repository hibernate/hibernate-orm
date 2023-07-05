/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.Incubating;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.spi.HibernateCriteriaBuilderDelegate;
import org.hibernate.query.sqm.FetchClauseType;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A utility class that makes it easier to build {@linkplain CriteriaQuery criteria queries}.
 * From within an implementation of the {@link #define()} method, all operations of the
 * {@link CriteriaBuilder} and {@link CriteriaQuery} may be called without the need for
 * specifying the target object.
 * <p>
 * For example:
 * <pre>
 * sessionFactory.inTransaction(session -> {
 *     List&lt;Book&gt; books
 *             = new CriteriaDefinition&lt;&gt;(session, Book.class) {
 *                 public void define() {
 *                     var book = from(Book.class);
 *                     where(like(book.get(Book_.title), "%Hibernate%"));
 *                     orderBy(desc(book.get(Book_.publicationDate)), asc(book.get(Book_.isbn)));
 *                     book.fetch(Book_.authors);
 *                 }
 *             }
 *             .createSelectionQuery()
 *             .setMaxResults(10)
 *             .getResultList();
 * });
 * </pre>
 *
 *
 * @param <R> the query result type
 */
@Incubating
public abstract class CriteriaDefinition<R>
		extends HibernateCriteriaBuilderDelegate
		implements JpaCriteriaQuery<R> {

	private final JpaCriteriaQuery<R> query;

	public CriteriaDefinition(SessionFactory factory, Class<R> resultType) {
		super( factory.getCriteriaBuilder() );
		query = createQuery( resultType );
		define();
	}

	public CriteriaDefinition(Session session, Class<R> resultType) {
		this( session.getSessionFactory(), resultType );
	}

	public SelectionQuery<R> createSelectionQuery(Session session) {
		return session.createSelectionQuery(query);
	}

	public abstract void define();

	@Override
	public JpaCriteriaQuery<R> select(Selection<? extends R> selection) {
		return query.select(selection);
	}

	@Override
	public JpaCriteriaQuery<R> multiselect(Selection<?>... selections) {
		return query.multiselect(selections);
	}

	@Override
	public JpaCriteriaQuery<R> multiselect(List<Selection<?>> list) {
		return query.multiselect(list);
	}

	@Override
	public JpaCriteriaQuery<R> where(Expression<Boolean> restriction) {
		return query.where(restriction);
	}

	@Override
	public JpaCriteriaQuery<R> where(Predicate... restrictions) {
		return query.where(restrictions);
	}

	@Override
	public JpaCriteriaQuery<R> groupBy(Expression... grouping) {
		return query.groupBy(grouping);
	}

	@Override
	public JpaCriteriaQuery<R> groupBy(List<Expression<?>> grouping) {
		return query.groupBy(grouping);
	}

	@Override
	public JpaCriteriaQuery<R> having(Expression<Boolean> restriction) {
		return query.having(restriction);
	}

	@Override
	public JpaCriteriaQuery<R> having(Predicate... restrictions) {
		return query.having(restrictions);
	}

	@Override
	public JpaCriteriaQuery<R> orderBy(Order... o) {
		return query.orderBy(o);
	}

	@Override
	public JpaCriteriaQuery<R> orderBy(List<Order> o) {
		return query.orderBy(o);
	}

	@Override
	public JpaCriteriaQuery<R> distinct(boolean distinct) {
		return query.distinct(distinct);
	}

	@Override
	public List<Order> getOrderList() {
		return query.getOrderList();
	}

	@Override
	public Set<ParameterExpression<?>> getParameters() {
		return query.getParameters();
	}

	@Override
	public <X> JpaRoot<X> from(Class<X> entityClass) {
		return query.from(entityClass);
	}

	@Override
	public <X> JpaRoot<X> from(EntityType<X> entity) {
		return query.from(entity);
	}

	@Override
	public <U> JpaSubQuery<U> subquery(Class<U> type) {
		return query.subquery(type);
	}

	@Override
	public Set<Root<?>> getRoots() {
		return query.getRoots();
	}

	@Override
	public JpaSelection<R> getSelection() {
		return query.getSelection();
	}

	@Override
	public List<Expression<?>> getGroupList() {
		return query.getGroupList();
	}

	@Override
	public JpaPredicate getGroupRestriction() {
		return query.getGroupRestriction();
	}

	@Override
	public boolean isDistinct() {
		return query.isDistinct();
	}

	@Override
	public Class<R> getResultType() {
		return query.getResultType();
	}

	@Override
	public JpaPredicate getRestriction() {
		return query.getRestriction();
	}

	@Override
	public JpaExpression<Number> getOffset() {
		return query.getOffset();
	}

	@Override
	public JpaCriteriaQuery<R> offset(JpaExpression<? extends Number> offset) {
		return query.offset(offset);
	}

	@Override
	public JpaCriteriaQuery<R> offset(Number offset) {
		return query.offset(offset);
	}

	@Override
	public JpaExpression<Number> getFetch() {
		return query.getFetch();
	}

	@Override
	public JpaCriteriaQuery<R> fetch(JpaExpression<? extends Number> fetch) {
		return query.fetch(fetch);
	}

	@Override
	public JpaCriteriaQuery<R> fetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		return query.fetch(fetch, fetchClauseType);
	}

	@Override
	public JpaCriteriaQuery<R> fetch(Number fetch) {
		return query.fetch(fetch);
	}

	@Override
	public JpaCriteriaQuery<R> fetch(Number fetch, FetchClauseType fetchClauseType) {
		return query.fetch(fetch, fetchClauseType);
	}

	@Override
	public FetchClauseType getFetchClauseType() {
		return query.getFetchClauseType();
	}

	@Override
	public List<Root<?>> getRootList() {
		return query.getRootList();
	}

	@Override
	public Collection<? extends JpaCteCriteria<?>> getCteCriterias() {
		return query.getCteCriterias();
	}

	@Override
	public <T> JpaCteCriteria<T> getCteCriteria(String cteName) {
		return query.getCteCriteria(cteName);
	}

	@Override
	public <T> JpaCteCriteria<T> with(AbstractQuery<T> criteria) {
		return query.with(criteria);
	}

	@Override
	public <T> JpaCteCriteria<T> withRecursiveUnionAll(AbstractQuery<T> baseCriteria, Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer) {
		return query.withRecursiveUnionAll(baseCriteria, recursiveCriteriaProducer);
	}

	@Override
	public <T> JpaCteCriteria<T> withRecursiveUnionDistinct(AbstractQuery<T> baseCriteria, Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer) {
		return query.withRecursiveUnionDistinct(baseCriteria, recursiveCriteriaProducer);
	}

	@Override
	public <T> JpaCteCriteria<T> with(String name, AbstractQuery<T> criteria) {
		return query.with(name, criteria);
	}

	@Override
	public <T> JpaCteCriteria<T> withRecursiveUnionAll(
			String name, AbstractQuery<T> baseCriteria,
			Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer) {
		return query.withRecursiveUnionAll(name, baseCriteria, recursiveCriteriaProducer);
	}

	@Override
	public <T> JpaCteCriteria<T> withRecursiveUnionDistinct(
			String name, AbstractQuery<T> baseCriteria,
			Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer) {
		return query.withRecursiveUnionDistinct(name, baseCriteria, recursiveCriteriaProducer);
	}

	@Override
	public JpaQueryStructure<R> getQuerySpec() {
		return query.getQuerySpec();
	}

	@Override
	public JpaQueryPart<R> getQueryPart() {
		return query.getQueryPart();
	}

	@Override
	public <X> JpaDerivedRoot<X> from(Subquery<X> subquery) {
		return query.from(subquery);
	}

	@Override
	public <X> JpaRoot<X> from(JpaCteCriteria<X> cte) {
		return query.from(cte);
	}
}
