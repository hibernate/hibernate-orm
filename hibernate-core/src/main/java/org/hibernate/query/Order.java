/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hibernate.query.SortDirection.ASCENDING;
import static org.hibernate.query.SortDirection.DESCENDING;

/**
 * A rule for sorting a query result set. This allows query result ordering
 * rules to be passed around the system before being applied to a
 * {@link org.hibernate.query.specification.QuerySpecification} by calling
 * {@link org.hibernate.query.specification.SelectionSpecification#sort(Order)
 * sort()}.
 * <pre>
 * SelectionSpecification.create(Book.class,
 *             "from Book b join b.authors a where a.name = :name")
 *         .sort(asc(Book_.publicationDate))
 *         .createQuery(session)
 *         .setParameter("name", authorName)
 *         .getResultList();
 * </pre>
 * <p>
 * {@code Order}s may be stacked using {@link List#of} and
 * {@link org.hibernate.query.specification.SelectionSpecification#resort(List)
 * resort()}.
 * <pre>
 * SelectionSpecification.create(Book.class,
 *             "from Book b join b.authors a where a.name = :name")
 *         .sort(List.of(asc(Book_.publicationDate), desc(Book_.ssn)))
 *         .setParameter("name", authorName)
 *         .getResultList();
 * </pre>
 * <p>
 * A parameter of a {@linkplain org.hibernate.annotations.processing.Find
 * finder method} or {@linkplain org.hibernate.annotations.processing.HQL
 * HQL query method} may be declared with type {@code Order<? super E>},
 * {@code List<Order<? super E>>}, or {@code Order<? super E>...} (varargs)
 * where {@code E} is the entity type returned by the query.
 *
 * @param <X> The result type of the query to be sorted
 *
 * @apiNote This class is similar to {@code jakarta.data.Sort}, and is
 *          used by Hibernate Data Repositories to implement Jakarta Data
 *          query methods.
 *
 * @see org.hibernate.query.specification.SelectionSpecification#sort(Order)
 * @see org.hibernate.query.specification.SelectionSpecification#resort(List)
 * @see org.hibernate.query.restriction.Restriction
 *
 * @author Gavin King
 *
 * @since 6.3
 */
@Incubating
public interface Order<X> {

	/**
	 * An order where an entity is sorted by the given attribute,
	 * with smaller values first. If the given attribute is of textual
	 * type, the ordering is case-sensitive.
	 */
	static <T> Order<T> asc(SingularAttribute<T,?> attribute) {
		return new AttributeOrder<>(ASCENDING, Nulls.NONE, attribute);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * with larger values first. If the given attribute is of textual
	 * type, the ordering is case-sensitive.
	 */
	static <T> Order<T> desc(SingularAttribute<T,?> attribute) {
		return new AttributeOrder<>(DESCENDING, Nulls.NONE, attribute);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * in the given direction. If the given attribute is of textual
	 * type, the ordering is case-sensitive.
	 */
	static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction) {
		return new AttributeOrder<>(direction, Nulls.NONE, attribute);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * in the given direction, with the specified case-sensitivity.
	 */
	static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction, boolean ignoreCase) {
		return new AttributeOrder<>(direction, Nulls.NONE, attribute, !ignoreCase);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * in the given direction, with the specified precedence for
	 * null values. If the given attribute is of textual type, the
	 * ordering is case-sensitive.
	 */
	static <T> Order<T> by(SingularAttribute<T, ?> attribute, SortDirection direction, Nulls nullPrecedence) {
		return new AttributeOrder<>(direction, nullPrecedence, attribute);
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, with smaller values first. If
	 * the named attribute is of textual type, the ordering is
	 * case-sensitive.
	 */
	static <T> Order<T> asc(Class<T> entityClass, String attributeName) {
		return new NamedAttributeOrder<>( ASCENDING, Nulls.NONE, entityClass, attributeName );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, with larger values first. If
	 * the named attribute is of textual type, the ordering is
	 * case-sensitive.
	 */
	static <T> Order<T> desc(Class<T> entityClass, String attributeName) {
		return new NamedAttributeOrder<>( DESCENDING, Nulls.NONE, entityClass, attributeName );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, in the given direction. If the
	 * named attribute is of textual type, the ordering is
	 * case-sensitive.
	 */
	static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction) {
		return new NamedAttributeOrder<>( direction, Nulls.NONE, entityClass, attributeName );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, in the given direction, with
	 * the specified case-sensitivity.
	 */
	static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction, boolean ignoreCase) {
		return new NamedAttributeOrder<>( direction, Nulls.NONE, entityClass, attributeName, !ignoreCase );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, in the given direction. If the
	 * named attribute is of textual type, with the specified
	 * precedence for null values. If the named attribute is of
	 * textual type, the ordering is case-sensitive.
	 */
	static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction, Nulls nullPrecedence) {
		return new NamedAttributeOrder<>( direction, nullPrecedence, entityClass, attributeName );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position with smaller values first. If the
	 * item is of textual type, the ordering is case-sensitive.
	 */
	static Order<Object[]> asc(int element) {
		return new ElementOrder<>( ASCENDING, Nulls.NONE, element );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position with larger values first. If the
	 * item is of textual type, the ordering is case-sensitive.
	 */
	static Order<Object[]> desc(int element) {
		return new ElementOrder<>( DESCENDING, Nulls.NONE, element );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position, in the given direction. If the item
	 * is of textual type, the ordering is case-sensitive.
	 */
	static Order<Object[]> by(int element, SortDirection direction) {
		return new ElementOrder<>( direction, Nulls.NONE, element );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position in the given direction, with the specified
	 * case-sensitivity.
	 */
	static Order<Object[]> by(int element, SortDirection direction, boolean ignoreCase) {
		return new ElementOrder<>( direction, Nulls.NONE, element, !ignoreCase );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position in the given direction, with the specified
	 * precedence for null values. If the named attribute is of
	 * textual type, the ordering is case-sensitive.
	 */
	static Order<Object[]> by(int element, SortDirection direction, Nulls nullPrecedence) {
		return new ElementOrder<>( direction, nullPrecedence, element );
	}

	/**
	 * The direction, {@linkplain SortDirection#ASCENDING ascending} or
	 * {@linkplain SortDirection#DESCENDING descending}, in which results
	 * are sorted.
	 *
	 * @since 7
	 */
	SortDirection direction();

	/**
	 * The {@linkplain Nulls ordering of null values}.
	 *
	 * @since 7
	 */
	Nulls nullPrecedence();

	/**
	 * For a lexicographic order based on textual values, whether case
	 * is significant.
	 *
	 * @since 7
	 */
	boolean caseSensitive();

	/**
	 * For an order based on an entity attribute, the entity class which
	 * declares the {@linkplain #attribute attribute}.
	 *
	 * @return the Java class which declares the attribute, or {@code null}
	 *         if this order is not based on an attribute
	 *
	 * @since 7
	 */
	Class<X> entityClass();

	/**
	 * For an order based on an entity attribute, the name of the
	 * {@linkplain #attribute attribute}.
	 *
	 * @return the name of the attribute, or {@code null} if this order is
	 *         not based on an attribute
	 *
	 * @since 7
	 */
	String attributeName();

	/**
	 * For an order based on an entity attribute, the metamodel object
	 * representing the attribute.
	 *
	 * @return the attribute, or {@code null} if this order is not based
	 *         on an attribute, or if only the name of the attribute was
	 *         specified
	 *
	 * @since 7
	 */
	SingularAttribute<X, ?> attribute();

	/**
	 * For an order based on an indexed element of the select clause,
	 * the index of the element.
	 *
	 * @return the index, or {@code 1} is this order is based on an
	 *         entity attribute
	 *
	 * @since 7
	 */
	int element();

	/**
	 * @return this order, but with the sorting direction reversed.
	 * @since 6.5
	 */
	Order<X> reverse();

	/**
	 * @return this order, but without case-sensitivity.
	 * @since 6.5
	 */
	Order<X> ignoringCase();

	/**
	 * @return this order, but with nulls sorted first.
	 * @since 6.5
	 */
	Order<X> withNullsFirst();

	/**
	 * @return this order, but with nulls sorted last.
	 * @since 6.5
	 */
	Order<X> withNullsLast();

	/**
	 * An order based on this order, possibly reversed.
	 *
	 * @param reverse {@code true} if the returned order should be
	 *                {@linkplain #reverse reversed}
	 * @return this order, but reversed if the argument is {@code true}
	 *
	 * @apiNote This is a convenience for use with Jakarta Data
	 *
	 * @since 7.0
	 */
	default Order<X> reversedIf(boolean reverse) {
		return reverse ? reverse() : this;
	}

	/**
	 * An order based on this order, possibly without case-sensitivity.
	 *
	 * @param ignoreCase {@code true} if this order should
	 *                   {@linkplain #ignoringCase ignore case}
	 * @return this order, but ignoring case if the argument is {@code true}
	 *
	 * @apiNote This is a convenience for use with Jakarta Data
	 *
	 * @since 7.0
	 */
	default Order<X> ignoringCaseIf(boolean ignoreCase) {
		return ignoreCase ? ignoringCase() : this;
	}

	/**
	 * Reverse the direction of the given ordering list
	 *
	 * @param ordering a list of {@code Order} items
	 * @return a new list, with each {@code Order} reversed
	 *
	 * @see #reverse()
	 *
	 * @since 6.5
	 */
	static <T> List<Order<? super T>> reverse(List<Order<? super T>> ordering) {
		return ordering.stream().map(Order::reverse).collect(toList());
	}

	@Deprecated(since = "7", forRemoval = true)
	default SortDirection getDirection() {
		return direction();
	}

	@Deprecated(since = "7", forRemoval = true)
	default Nulls getNullPrecedence() {
		return nullPrecedence();
	}

	@Deprecated(since = "7", forRemoval = true)
	default boolean isCaseInsensitive() {
		return !caseSensitive();
	}

	@Deprecated(since = "7", forRemoval = true)
	default SingularAttribute<X, ?> getAttribute() {
		return attribute();
	}

	@Deprecated(since = "7", forRemoval = true)
	default Class<X> getEntityClass() {
		return entityClass();
	}

	@Deprecated(since = "7", forRemoval = true)
	default String getAttributeName() {
		return attributeName();
	}

	@Deprecated(since = "7", forRemoval = true)
	default int getElement() {
		return element();
	}
}
