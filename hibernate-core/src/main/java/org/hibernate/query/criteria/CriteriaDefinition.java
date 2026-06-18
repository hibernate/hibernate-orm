/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.spi.HibernateCriteriaBuilderDelegate;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A utility class that makes it easier to build {@linkplain CriteriaQuery criteria queries}.
 * From within an initializer block of a (usually anonymous) subclass, all operations of the
 * {@link CriteriaBuilder} and {@link CriteriaQuery} may be called without the need for
 * specifying the target object.
 * <p>
 * For example:
 * <pre>
 * sessionFactory.inTransaction(session -&gt; {
 *     List&lt;Book&gt; books
 *             = new CriteriaDefinition&lt;&gt;(sessionFactory, Book.class) {{
 *                 var book = from(Book.class);
 *                 where(like(book.get(Book_.title), "%Hibernate%"));
 *                 orderBy(desc(book.get(Book_.publicationDate)), asc(book.get(Book_.isbn)));
 *                 book.fetch(Book_.authors);
 *             }}
 *             .createSelectionQuery(session)
 *             .setMaxResults(10)
 *             .getResultList();
 *     ...
 * });
 * </pre>
 * <p>
 * A {@code CriteriaDefinition} may even be used to modify a base HQL or criteria query:
 * <pre>
 * sessionFactory.inTransaction(session -&gt; {
 *     List&lt;Book&gt; books
 *             = new CriteriaDefinition&lt;&gt;(sessionFactory, Book.class,
 *                     "from Book left join fetch authors where type = BOOK") {{
 *                 var book = getRoot(0, Book.class);
 *                 where(getRestriction(), like(book.get(Book_.title), "%Hibernate%"));
 *                 orderBy(desc(book.get(Book_.publicationDate)), asc(book.get(Book_.isbn)));
 *             }}
 *             .createSelectionQuery(session)
 *             .getResultList();
 *     ...
 * });
 * </pre>
 * For queries which don't change between executions, the {@code CriteriaDefinition} may be
 * safely built and cached at startup:
 * <pre>
 * // build and cache the query
 * static final CriteriaQuery&lt;Book&gt; bookQuery =
 *         new CriteriaDefinition&lt;&gt;(sessionFactory, Book.class) {{
 *             var book = from(Book.class);
 *             where(like(book.get(Book_.title), "%Hibernate%"));
 *             orderBy(desc(book.get(Book_.publicationDate)), asc(book.get(Book_.isbn)));
 *             book.fetch(Book_.authors);
 *         }};
 *
 * ...
 *
 * // execute it in a session
 * sessionFactory.inTransaction(session -&gt; {
 *     List&lt;Book&gt; books =
 *             session.createQuery(bookQuery)
 *                     .setMaxResults(10)
 *                     .getResultList();
 *     ...
 * });
 * </pre>
 * A {@code CriteriaDefinition} may be used to modify another {@code CriteriaDefinition}:
 * <pre>
 * var bookFilter
 *         = new CriteriaDefinition&lt;&gt;(sessionFactory, Book.class) {{
 *             where(like(from(Book.class).get(Book_.title), "%Hibernate%"));
 *         }};
 * long count
 *         = new CriteriaDefinition&lt;&gt;(bookFilter, Long.class) {{
 *             select(count());
 *         }}
 *         .createSelectionQuery(session)
 *         .getSingleResult();
 * var books =
 *         = new CriteriaDefinition&lt;&gt;(bookFilter) {{
 *             var book = (Root&lt;Book&gt;) getRootList().get(0);
 *             book.fetch(Book_.authors);
 *             orderBy(desc(book.get(Book_.publicationDate)), asc(book.get(Book_.isbn)));
 *         }}
 *         .createSelectionQuery(session)
 *         .setMaxResults(10)
 *         .getResultList();
 * </pre>
 *
 * @param <R> the query result type
 *
 * @since 6.3
 *
 * @author Gavin King
 */
@Incubating
public abstract class CriteriaDefinition<R>
		extends HibernateCriteriaBuilderDelegate
		implements JpaCriteriaQuery<R> {

	private final JpaCriteriaQuery<R> query;

	/**
	 * Construct a new {@code CriteriaDefinition} based on the given
	 * {@code CriteriaDefinition}, with the same query return type.
	 *
	 * @param template the original query
	 *
	 * @since 7.0
	 */
	public CriteriaDefinition(CriteriaDefinition<R> template) {
		super( template.getCriteriaBuilder() );
		query = ((SqmSelectStatement<R>) template.query)
				.copy( SqmCopyContext.simpleContext() );
	}

	/**
	 * Construct a new {@code CriteriaDefinition} based on the given
	 * {@code CriteriaDefinition}. This overload permits changing the
	 * query return type. It is expected that {@link #select} be called
	 * to rewrite the selection list.
	 *
	 * @param template the original query
	 * @param resultType the new return type
	 *
	 * @since 7.0
	 */
	public CriteriaDefinition(CriteriaDefinition<?> template, Class<R> resultType) {
		super( template.getCriteriaBuilder() );
		if ( !(template.query instanceof SqmSelectStatement<?> selectStatement) ) {
			throw new IllegalArgumentException( "Not a SqmSelectStatement" );
		}
		query = selectStatement.createCopy( SqmCopyContext.simpleContext(), resultType );
	}

	public CriteriaDefinition(SessionFactory factory, Class<R> resultType) {
		super( factory.getCriteriaBuilder() );
		query = createQuery( resultType );
	}

	public CriteriaDefinition(SessionFactory factory, Class<R> resultType, String baseHql) {
		super( factory.getCriteriaBuilder() );
		query = createQuery( baseHql, resultType );
	}

	public CriteriaDefinition(SessionFactory factory, CriteriaQuery<R> baseQuery) {
		super( factory.getCriteriaBuilder() );
		query = (JpaCriteriaQuery<R>) baseQuery;
	}

	public CriteriaDefinition(CriteriaQuery<R> baseQuery) {
		super( ((JpaCriteriaQuery<R>) baseQuery).getCriteriaBuilder() );
		query = (JpaCriteriaQuery<R>) baseQuery;
	}

	public CriteriaDefinition(EntityManagerFactory factory, Class<R> resultType) {
		super( factory.getCriteriaBuilder() );
		query = createQuery( resultType );
	}

	public CriteriaDefinition(EntityManagerFactory factory, Class<R> resultType, String baseHql) {
		super( factory.getCriteriaBuilder() );
		query = createQuery( baseHql, resultType );
	}

	public CriteriaDefinition(EntityManagerFactory factory, CriteriaQuery<R> baseQuery) {
		super( factory.getCriteriaBuilder() );
		query = (JpaCriteriaQuery<R>) baseQuery;
	}

	public CriteriaDefinition(SharedSessionContract session, Class<R> resultType) {
		this( session.getFactory(), resultType );
	}

	public CriteriaDefinition(SharedSessionContract session, Class<R> resultType, String baseHql) {
		this( session.getFactory(), resultType, baseHql );
	}

	public CriteriaDefinition(SharedSessionContract session, CriteriaQuery<R> baseQuery) {
		this( session.getFactory(), baseQuery );
	}

	public CriteriaDefinition(EntityManager entityManager, Class<R> resultType) {
		this( entityManager.getEntityManagerFactory(), resultType );
	}

	public CriteriaDefinition(EntityManager entityManager, Class<R> resultType, String baseHql) {
		this( entityManager.getEntityManagerFactory(), resultType, baseHql );
	}

	public CriteriaDefinition(EntityManager entityManager, CriteriaQuery<R> baseQuery) {
		this( entityManager.getEntityManagerFactory(), baseQuery );
	}

	@Nonnull
	public SelectionQuery<R> createSelectionQuery(SharedSessionContract session) {
		return session.createQuery( query );
	}

	@Nonnull
	public TypedQuery<R> createQuery(@Nonnull EntityManager entityManager) {
		return entityManager.createQuery( query );
	}

	@Incubating
	@Nonnull
	public JpaCriteriaQuery<R> restrict(@Nonnull Predicate predicate) {
		final JpaPredicate existing = getRestriction();
		return existing == null ? where( predicate ) : where( existing, predicate );
	}

	@Override
	@Nonnull
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return query.getCriteriaBuilder();
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> select(@Nonnull Selection<? extends R> selection) {
		return query.select(selection);
	}

	@Nonnull
	@Override @Deprecated
	public JpaCriteriaQuery<R> multiselect(@Nonnull Selection<?>... selections) {
		return query.multiselect(selections);
	}

	@Nonnull
	@Override @Deprecated
	public JpaCriteriaQuery<R> multiselect(@Nonnull List<Selection<?>> list) {
		return query.multiselect(list);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> where(@Nonnull Expression<Boolean> restriction) {
		return query.where(restriction);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> groupBy(@Nonnull Expression... grouping) {
		return query.groupBy(grouping);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> groupBy(@Nonnull List<Expression<?>> grouping) {
		return query.groupBy(grouping);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> having(@Nonnull Expression<Boolean> restriction) {
		return query.having(restriction);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> orderBy(@Nonnull Order... o) {
		return query.orderBy(o);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> orderBy(@Nonnull List<Order> o) {
		return query.orderBy(o);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> distinct(boolean distinct) {
		return query.distinct(distinct);
	}

	@Nonnull
	@Override
	public List<Order> getOrderList() {
		return query.getOrderList();
	}

	@Nonnull
	@Override
	public Set<ParameterExpression<?>> getParameters() {
		return query.getParameters();
	}

	@Nonnull
	@Override
	public <X> JpaRoot<X> from(@Nonnull Class<X> entityClass) {
		return query.from(entityClass);
	}

	@Nonnull
	@Override
	public <X> JpaRoot<X> from(@Nonnull EntityType<X> entity) {
		return query.from(entity);
	}

	@Nonnull
	@Override
	public <U> JpaSubQuery<U> subquery(@Nonnull Class<U> type) {
		return query.subquery(type);
	}

	@Nonnull
	@Override
	public Set<Root<?>> getRoots() {
		return query.getRoots();
	}

	@Nullable
	@Override
	public JpaSelection<R> getSelection() {
		return query.getSelection();
	}

	@Nonnull
	@Override
	public List<Expression<?>> getGroupList() {
		return query.getGroupList();
	}

	@Nullable
	@Override
	public JpaPredicate getGroupRestriction() {
		return query.getGroupRestriction();
	}

	@Override
	public boolean isDistinct() {
		return query.isDistinct();
	}

	@Nonnull
	@Override
	public Class<R> getResultType() {
		return query.getResultType();
	}

	@Nullable
	@Override
	public JpaPredicate getRestriction() {
		return query.getRestriction();
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> where(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return query.where(restrictions);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> where(@Nonnull BooleanExpression... restrictions) {
		return query.where(restrictions);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> having(@Nonnull BooleanExpression... restrictions) {
		return query.having(restrictions);
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<R> having(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return query.having(restrictions);
	}

	@Nonnull
	@Override
	public <U> JpaSubQuery<U> subquery(@Nonnull EntityType<U> type) {
		return query.subquery( type );
	}

	@Nullable
	@Override
	public JpaExpression<Number> getOffset() {
		return query.getOffset();
	}

	@Override
	@Nonnull
	public JpaCriteriaQuery<R> offset(@Nullable JpaExpression<? extends Number> offset) {
		return query.offset(offset);
	}

	@Override
	@Nonnull
	public JpaCriteriaQuery<R> offset(@Nullable Number offset) {
		return query.offset(offset);
	}

	@Nullable
	@Override
	public JpaExpression<Number> getFetch() {
		return query.getFetch();
	}

	@Override
	@Nonnull
	public JpaCriteriaQuery<R> fetch(@Nullable JpaExpression<? extends Number> fetch) {
		return query.fetch(fetch);
	}

	@Override
	@Nonnull
	public JpaCriteriaQuery<R> fetch(@Nullable JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		return query.fetch(fetch, fetchClauseType);
	}

	@Override
	@Nonnull
	public JpaCriteriaQuery<R> fetch(@Nullable Number fetch) {
		return query.fetch(fetch);
	}

	@Override
	@Nonnull
	public JpaCriteriaQuery<R> fetch(@Nullable Number fetch, FetchClauseType fetchClauseType) {
		return query.fetch(fetch, fetchClauseType);
	}

	@Override
	public FetchClauseType getFetchClauseType() {
		return query.getFetchClauseType();
	}

	@Override
	@Nonnull
	public List<Root<?>> getRootList() {
		return query.getRootList();
	}

	@Override
	@Nonnull
	public <E> JpaRoot<? extends E> getRoot(int position, Class<E> type) {
		return query.getRoot( position, type );
	}

	@Override
	@Nonnull
	public <E> JpaRoot<? extends E> getRoot(String alias, Class<E> type) {
		return query.getRoot( alias, type );
	}

	@Override
	@Nonnull
	public Collection<? extends JpaCteCriteria<?>> getCteCriterias() {
		return query.getCteCriterias();
	}

	@Override
	public <T> @Nullable JpaCteCriteria<T> getCteCriteria(@Nonnull String cteName) {
		return query.getCteCriteria(cteName);
	}

	@Override
	@Deprecated(since = "7", forRemoval = true)
	@SuppressWarnings("removal")
	@Nonnull
	public <T> JpaCteCriteria<T> with(@Nonnull AbstractQuery<T> criteria) {
		return query.with(criteria);
	}

	@Override
	@Nonnull
	public <T> JpaCteCriteria<T> withRecursiveUnionAll(
			@Nonnull AbstractQuery<T> baseCriteria,
			@Nonnull Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer) {
		return query.withRecursiveUnionAll(baseCriteria, recursiveCriteriaProducer);
	}

	@Override
	@Nonnull
	public <T> JpaCteCriteria<T> withRecursiveUnionDistinct(
			@Nonnull AbstractQuery<T> baseCriteria,
			@Nonnull Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer) {
		return query.withRecursiveUnionDistinct(baseCriteria, recursiveCriteriaProducer);
	}

	@Override
	@Nonnull
	public <T> JpaCteCriteria<T> with(@Nonnull String name, @Nonnull AbstractQuery<T> criteria) {
		return query.with(name, criteria);
	}

	@Override
	@Nonnull
	public <T> JpaCteCriteria<T> withRecursiveUnionAll(
			@Nonnull String name, @Nonnull AbstractQuery<T> baseCriteria,
			@Nonnull Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer) {
		return query.withRecursiveUnionAll(name, baseCriteria, recursiveCriteriaProducer);
	}

	@Override
	@Nonnull
	public <T> JpaCteCriteria<T> withRecursiveUnionDistinct(
			@Nonnull String name, @Nonnull AbstractQuery<T> baseCriteria,
			@Nonnull Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer) {
		return query.withRecursiveUnionDistinct(name, baseCriteria, recursiveCriteriaProducer);
	}

	@Override
	@Nonnull
	public JpaQueryStructure<R> getQuerySpec() {
		return query.getQuerySpec();
	}

	@Override
	@Nonnull
	public JpaQueryPart<R> getQueryPart() {
		return query.getQueryPart();
	}

	@Nonnull
	@Override
	public <X> JpaDerivedRoot<X> from(@Nonnull Subquery<X> subquery) {
		return query.from(subquery);
	}

	@Override
	@Nonnull
	public <X> JpaRoot<X> from(@Nonnull JpaCteCriteria<X> cte) {
		return query.from(cte);
	}

	@Override
	@Nonnull
	public <X> JpaFunctionRoot<X> from(@Nonnull JpaSetReturningFunction<X> function) {
		return query.from( function );
	}

	@Override
	@Nonnull
	public JpaCriteriaQuery<Long> createCountQuery() {
		return query.createCountQuery();
	}

	@Override
	@Nonnull
	public JpaCriteriaQuery<Boolean> createExistsQuery() {
		return query.createExistsQuery();
	}
}
