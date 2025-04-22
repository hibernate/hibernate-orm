/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.hibernate.query.SortDirection.ASCENDING;
import static org.hibernate.query.SortDirection.DESCENDING;

/**
 * A rule for sorting a query result set.
 * <p>
 * This is a convenience class which allows query result ordering rules to be
 * passed around the system before being applied to a {@link Query} by calling
 * {@link org.hibernate.query.programmatic.SelectionSpecification#sort(Order)}.
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
 * {@link org.hibernate.query.programmatic.SelectionSpecification#resort(List)}.
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
 * @see org.hibernate.query.programmatic.SelectionSpecification#sort(Order)
 * @see org.hibernate.query.programmatic.SelectionSpecification#resort(List)
 * @see org.hibernate.query.restriction.Restriction
 *
 * @author Gavin King
 *
 * @since 6.3
 */
@Incubating
public class Order<X> {
	private final SortDirection order;
	private final SingularAttribute<X,?> attribute;
	private final Class<X> entityClass;
	private final String attributeName;
	private final Nulls nullPrecedence;
	private final int element;
	private final boolean ignoreCase;

	private Order(SortDirection order, Nulls nullPrecedence, SingularAttribute<X, ?> attribute) {
		this.order = order;
		this.attribute = attribute;
		this.attributeName = attribute.getName();
		this.entityClass = attribute.getDeclaringType().getJavaType();
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = false;
	}

	private Order(SortDirection order, Nulls nullPrecedence, SingularAttribute<X, ?> attribute, boolean ignoreCase) {
		this.order = order;
		this.attribute = attribute;
		this.attributeName = attribute.getName();
		this.entityClass = attribute.getDeclaringType().getJavaType();
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = ignoreCase;
	}

	private Order(SortDirection order, Nulls nullPrecedence, Class<X> entityClass, String attributeName) {
		this.order = order;
		this.entityClass = entityClass;
		this.attributeName = attributeName;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = false;
	}

	private Order(SortDirection order, Nulls nullPrecedence, int element) {
		this.order = order;
		this.entityClass = null;
		this.attributeName = null;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = element;
		this.ignoreCase = false;
	}

	private Order(SortDirection order, Nulls nullPrecedence, Class<X> entityClass, String attributeName, boolean ignoreCase) {
		this.order = order;
		this.entityClass = entityClass;
		this.attributeName = attributeName;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = ignoreCase;
	}

	private Order(SortDirection order, Nulls nullPrecedence, int element, boolean ignoreCase) {
		this.order = order;
		this.entityClass = null;
		this.attributeName = null;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = element;
		this.ignoreCase = ignoreCase;
	}

	private Order(Order<X> other, SortDirection order) {
		this.order = order;
		this.attribute = other.attribute;
		this.entityClass = other.entityClass;
		this.attributeName = other.attributeName;
		this.nullPrecedence = other.nullPrecedence;
		this.element = other.element;
		this.ignoreCase = other.ignoreCase;
	}

	private Order(Order<X> other, boolean ignoreCase) {
		this.order = other.order;
		this.attribute = other.attribute;
		this.entityClass = other.entityClass;
		this.attributeName = other.attributeName;
		this.nullPrecedence = other.nullPrecedence;
		this.element = other.element;
		this.ignoreCase = ignoreCase;
	}

	private Order(Order<X> other, Nulls nullPrecedence) {
		this.order = other.order;
		this.attribute = other.attribute;
		this.entityClass = other.entityClass;
		this.attributeName = other.attributeName;
		this.nullPrecedence = nullPrecedence;
		this.element = other.element;
		this.ignoreCase = other.ignoreCase;
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * with smaller values first. If the given attribute is of textual
	 * type, the ordering is case-sensitive.
	 */
	public static <T> Order<T> asc(SingularAttribute<T,?> attribute) {
		return new Order<>(ASCENDING, Nulls.NONE, attribute);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * with larger values first. If the given attribute is of textual
	 * type, the ordering is case-sensitive.
	 */
	public static <T> Order<T> desc(SingularAttribute<T,?> attribute) {
		return new Order<>(DESCENDING, Nulls.NONE, attribute);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * in the given direction. If the given attribute is of textual
	 * type, the ordering is case-sensitive.
	 */
	public static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction) {
		return new Order<>(direction, Nulls.NONE, attribute);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * in the given direction, with the specified case-sensitivity.
	 */
	public static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction, boolean ignoreCase) {
		return new Order<>(direction, Nulls.NONE, attribute, ignoreCase);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * in the given direction, with the specified precedence for
	 * null values. If the given attribute is of textual type, the
	 * ordering is case-sensitive.
	 */
	public static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction, Nulls nullPrecedence) {
		return new Order<>(direction, nullPrecedence, attribute);
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, with smaller values first. If
	 * the named attribute is of textual type, the ordering is
	 * case-sensitive.
	 */
	public static <T> Order<T> asc(Class<T> entityClass, String attributeName) {
		return new Order<>( ASCENDING, Nulls.NONE, entityClass, attributeName );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, with larger values first. If
	 * the named attribute is of textual type, the ordering is
	 * case-sensitive.
	 */
	public static <T> Order<T> desc(Class<T> entityClass, String attributeName) {
		return new Order<>( DESCENDING, Nulls.NONE, entityClass, attributeName );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, in the given direction. If the
	 * named attribute is of textual type, the ordering is
	 * case-sensitive.
	 */
	public static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction) {
		return new Order<>( direction, Nulls.NONE, entityClass, attributeName );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, in the given direction, with
	 * the specified case-sensitivity.
	 */
	public static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction, boolean ignoreCase) {
		return new Order<>( direction, Nulls.NONE, entityClass, attributeName, ignoreCase );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, in the given direction. If the
	 * named attribute is of textual type, with the specified
	 * precedence for null values. If the named attribute is of
	 * textual type, the ordering is case-sensitive.
	 */
	public static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction, Nulls nullPrecedence) {
		return new Order<>( direction, nullPrecedence, entityClass, attributeName );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position with smaller values first. If the
	 * item is of textual type, the ordering is case-sensitive.
	 */
	public static Order<Object[]> asc(int element) {
		return new Order<>( ASCENDING, Nulls.NONE, element );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position with larger values first. If the
	 * item is of textual type, the ordering is case-sensitive.
	 */
	public static Order<Object[]> desc(int element) {
		return new Order<>( DESCENDING, Nulls.NONE, element );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position, in the given direction. If the item
	 * is of textual type, the ordering is case-sensitive.
	 */
	public static Order<Object[]> by(int element, SortDirection direction) {
		return new Order<>( direction, Nulls.NONE, element );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position in the given direction, with the specified
	 * case-sensitivity.
	 */
	public static Order<Object[]> by(int element, SortDirection direction, boolean ignoreCase) {
		return new Order<>( direction, Nulls.NONE, element, ignoreCase );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position in the given direction, with the specified
	 * precedence for null values. If the named attribute is of
	 * textual type, the ordering is case-sensitive.
	 */
	public static Order<Object[]> by(int element, SortDirection direction, Nulls nullPrecedence) {
		return new Order<>( direction, nullPrecedence, element );
	}

	public SortDirection getDirection() {
		return order;
	}

	public Nulls getNullPrecedence() {
		return nullPrecedence;
	}

	public boolean isCaseInsensitive() {
		return ignoreCase;
	}

	public SingularAttribute<X, ?> getAttribute() {
		return attribute;
	}

	public Class<X> getEntityClass() {
		return entityClass;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public int getElement() {
		return element;
	}

	/**
	 * @return this order, but with the sorting direction reversed.
	 * @since 6.5
	 */
	public Order<X> reverse() {
		return new Order<>( this, order.reverse() );
	}

	/**
	 * @return this order, but without case-sensitivity.
	 * @since 6.5
	 */
	public Order<X> ignoringCase() {
		return new Order<>( this, true );
	}

	/**
	 * @return this order, but with nulls sorted first.
	 * @since 6.5
	 */
	public Order<X> withNullsFirst() {
		return new Order<>( this, Nulls.FIRST );
	}

	/**
	 * @return this order, but with nulls sorted last.
	 * @since 6.5
	 */
	public Order<X> withNullsLast() {
		return new Order<>( this, Nulls.LAST );
	}

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
	public Order<X> reversedIf(boolean reverse) {
		return reverse ? reverse() : this;
	}

	/**
	 * An order based on this order, possibly without case-sensitivity.
	 *
	 * @param ignoreCase {@code true} if this order should be
	 *                   {@linkplain #ignoringCase ignore case}
	 * @return this order, but ignoring case if the argument is {@code true}
	 *
	 * @apiNote This is a convenience for use with Jakarta Data
	 *
	 * @since 7.0
	 */
	public Order<X> ignoringCaseIf(boolean ignoreCase) {
		return ignoreCase ? ignoringCase() : this;
	}

	@Override
	public String toString() {
		return attributeName + " " + order;
	}

	@Override
	public boolean equals(Object object) {
		if ( object instanceof Order<?> that) {
			return that.order == this.order
				&& that.nullPrecedence == this.nullPrecedence
				&& that.ignoreCase == this.ignoreCase
				&& that.element == this.element
				&& Objects.equals( that.attributeName, this.attributeName )
				&& Objects.equals( that.entityClass, this.entityClass );
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( order, element, attributeName, entityClass );
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
	public static <T> List<Order<? super T>> reverse(List<Order<? super T>> ordering) {
		return ordering.stream().map(Order::reverse).collect(toList());
	}
}
