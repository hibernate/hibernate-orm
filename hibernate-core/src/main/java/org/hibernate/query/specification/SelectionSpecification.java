/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.Incubating;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.internal.SelectionSpecificationImpl;

import java.util.List;

/**
 * Specialization of {@link QuerySpecification} for programmatic customization of
 * {@linkplain SelectionQuery selection queries} with ordering and restriction criteria.
 * <ul>
 * <li>The method {@link #restrict(Restriction)} allows application of additional
 *     {@linkplain Restriction filtering} to the query results. The static factory methods
 *     of {@link Restriction} are used to express filtering criteria of various kinds.
 * <li>Refinement or replacement of the query sorting criteria is possible via the methods
 *     {@link #sort(Order)} and {@link #resort(List)}, together with the static
 *     factory methods of {@link Order}.
 * <li>The method {@link #fetch(Path)} adds the {@linkplain Path path} of an association
 *     to be fetched by the query.
 * </ul>
 * <p>
 * Once all {@linkplain #sort sorting} and {@linkplain #restrict restrictions}
 * are specified, call {@link #createQuery createQuery()} to obtain an
 * {@linkplain SelectionQuery executable selection query object}.
 * <pre>
 * SelectionSpecification.create(Book.class,
 *             "from Book where discontinued = false")
 *         .restrict(Restriction.contains(Book_.title, "hibernate", false))
 *         .sort(Order.desc(Book_.title))
 *         .fetch(Path.from(Book.class).to(Book_publisher))
 *         .createQuery(session)                       // obtain a SelectionQuery
 *         .setPage(Page.first(50))
 *         .getResultList();
 * </pre>
 * <p>
 * A {@code SelectionSpecification} always represents a query which returns a singe root
 * entity. The restriction and ordering criteria are interpreted as applying to the field
 * and properties of this root entity.
 * <p>
 * This interface, together with {@link Order} and {@link Page}, provides a streamlined
 * API for offset-based pagination. For example, given a list of {@code Order}s in
 * {@code orderList}, and the {@code currentPage}, we may write:
 * <pre>
 * SelectionSpecification.create(Book.class, "from Book where ... ")
 *         .resort(orderList)
 *         .createQuery(session)
 *                 .setPage(currentPage)
 *                 .getResultList();
 * </pre>
 *
 * @param <T> The entity type returned by the query
 *
 * @author Steve Ebersole
 *
 * @since 7.0
 */
@Incubating
public interface SelectionSpecification<T> extends QuerySpecification<T> {
	/**
	 * Adds an ordering to the selection specification.
	 * Appended to any previous ordering.
	 *
	 * @param order The ordering fragment to be added.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> sort(Order<? super T> order);

	/**
	 * Sets the ordering for this selection specification.
	 * If ordering was already defined, this method drops the previous ordering
	 * in favor of the passed {@code orders}.
	 *
	 * @param order The ordering fragment to be used.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> resort(Order<? super T> order);

	/**
	 * Sets the sorting for this selection specification.
	 * If sorting was already defined, this method drops the previous sorting
	 * in favor of the passed {@code orders}.
	 *
	 * @param orders The sorting fragments to be used.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> resort(List<? extends Order<? super T>> orders);

	@Override
	SelectionSpecification<T> restrict(Restriction<? super T> restriction);

	/**
	 * Add a fetch {@linkplain Path path} to the specification.
	 *
	 * @param fetchPath The path to fetch
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> fetch(Path<T,?> fetchPath);

	/**
	 * A function capable of modifying or augmenting a criteria query.
	 *
	 * @param <T> The root entity type
	 */
	@FunctionalInterface
	interface Augmentation<T> {
		void augment(CriteriaBuilder builder, CriteriaQuery<T> query, Root<T> root);
	}

	/**
	 * Add an {@linkplain Augmentation augmentation} to the specification.
	 * <p>
	 * For example:
	 * <pre>
	 * SelectionSpecification.create(Book.class)
	 *       .augment((builder, query, book) ->
	 *               // augment the query via JPA Criteria API
	 *               query.where(builder.like(book.get(Book_.title), titlePattern)),
	 *                           builder.greaterThan(book.get(Book_.pages), minPages))
	 *                   .orderBy(builder.asc(book.get(Book_.isbn)))
	 *       .createQuery(session)
	 *       .getResultList();
	 * </pre>
	 * For complicated cases, a {@link org.hibernate.query.criteria.CriteriaDefinition}
	 * may be used within an augmentation to eliminate repetitive explicit references to
	 * the {@link CriteriaBuilder}.
	 * <pre>
	 * SelectionSpecification.create(Book.class)
	 *       .augment((builder, query, book) ->
	 *           // eliminate explicit references to 'builder'
	 *           new CriteriaDefinition&lt;&gt;(query) {{
	 *               where(like(entity.get(BasicEntity_.title), titlePattern),
	 *                     greaterThan(book.get(Book_.pages), minPages));
	 *               orderBy(asc(book.get(Book_.isbn)));
	 *           }}
	 *       )
	 *       .createQuery(session)
	 *       .getResultList();
	 * </pre>
	 *
	 * @param augmentation A function capable of modifying or augmenting a criteria query.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> augment(Augmentation<T> augmentation);

	@Override
	SelectionQuery<T> createQuery(Session session);

	@Override
	SelectionQuery<T> createQuery(StatelessSession session);

	@Override
	SelectionQuery<T> createQuery(EntityManager entityManager);

	/**
	 * Build a {@link CriteriaQuery criteria query}
	 * satisfying this specification, using the given
	 * {@link CriteriaBuilder}.
	 * <p>
	 * If the returned criteria query is mutated, the mutations
	 * will not be not reflected in this specification.
	 *
	 * @return a new criteria query
	 */
	@Override
	CriteriaQuery<T> buildCriteria(CriteriaBuilder builder);

	@Override
	SelectionSpecification<T> validate(CriteriaBuilder builder);

	@Override
	TypedQueryReference<T> reference();

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain SelectionQuery} for the given entity type,
	 * allowing the addition of {@linkplain #sort sorting}
	 * and {@linkplain #restrict restrictions}.
	 * This is effectively the same as calling {@linkplain #create(Class, String)}
	 * with {@code "from {rootEntityType}"} as the HQL.
	 *
	 * @param rootEntityType The entity type which is the root of the query.
	 *
	 * @param <T> The entity type which is the root of the query.
	 * {@code resultType} and {@code <T>} are both expected to refer to a singular query root.
	 */
	static <T> SelectionSpecification<T> create(Class<T> rootEntityType) {
		return new SelectionSpecificationImpl<>( rootEntityType );
	}

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain SelectionQuery} based on a base HQL statement,
	 * allowing the addition of {@linkplain #sort sorting}
	 * and {@linkplain #restrict restrictions}.
	 *
	 * @param hql The base HQL query.
	 * @param resultType The result type which will ultimately be returned from the {@linkplain SelectionQuery}
	 *
	 * @param <T> The root entity type for the query.
	 * {@code resultType} and {@code <T>} are both expected to refer to a singular query root.
	 *
	 * @throws IllegalSelectQueryException The given HQL is expected to be a {@code select} query.  This method will
	 * throw an exception if not.
	 */
	static <T> SelectionSpecification<T> create(Class<T> resultType, String hql) {
		return new SelectionSpecificationImpl<>( hql, resultType );
	}

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain SelectionQuery} for the given criteria query,
	 * allowing the addition of {@linkplain #sort sorting}
	 * and {@linkplain #restrict restrictions}.
	 *
	 * @param criteria The criteria query
	 *
	 * @param <T> The entity type which is the root of the query.
	 */
	static <T> SelectionSpecification<T> create(CriteriaQuery<T> criteria) {
		return new SelectionSpecificationImpl<>( criteria );
	}

	/**
	 * Create a {@link ProjectionSpecification} allowing selection of specific
	 * {@linkplain ProjectionSpecification#select(SingularAttribute) fields}
	 * and {@linkplain ProjectionSpecification#select(Path) compound paths}.
	 * The returned projection holds a reference to this specification,
	 * and so mutation of this object also affects the projection.
	 *
	 * @return a new {@link ProjectionSpecification}
	 *
	 * @since 7.2
	 */
	@Incubating
	default ProjectionSpecification<T> createProjection() {
		return ProjectionSpecification.create( this );
	}

	/**
	 * Create a {@link SimpleProjectionSpecification} for the given
	 * {@linkplain ProjectionSpecification#select(SingularAttribute) field}.
	 * The returned projection holds a reference to this specification,
	 * and so mutation of this object also affects the projection.
	 *
	 * @return a new {@link SimpleProjectionSpecification}
	 *
	 * @since 7.2
	 */
	@Incubating
	default <X> SimpleProjectionSpecification<T,X> createProjection(SingularAttribute<? super T, X> attribute) {
		return SimpleProjectionSpecification.create( this, attribute );
	}

	/**
	 * Create a {@link SimpleProjectionSpecification} for the given
	 * {@linkplain ProjectionSpecification#select(Path) compound path}.
	 * The returned projection holds a reference to this specification,
	 * and so mutation of this object also affects the projection.
	 *
	 * @return a new {@link SimpleProjectionSpecification}
	 *
	 * @since 7.2
	 */
	@Incubating
	default <X> SimpleProjectionSpecification<T,X> createProjection(Path<T, X> path) {
		return SimpleProjectionSpecification.create( this, path );
	}
}
