/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

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
 * This is a convenience class which allows query result ordering
 * rules to be passed around the system before being applied to
 * a {@link Query} by calling {@link SelectionQuery#setOrder}.
 * <p>
 * A parameter of a {@linkplain org.hibernate.annotations.processing.Find
 * finder method} or {@linkplain org.hibernate.annotations.processing.HQL
 * HQL query method} may be declared with type {@code Order<? super E>},
 * {@code List<Order<? super E>>}, or {@code Order<? super E>...} (varargs)
 * where {@code E} is the entity type returned by the query.
 *
 * @param <X> The result type of the query to be sorted
 *
 * @see SelectionQuery#setOrder(Order)
 * @see SelectionQuery#setOrder(java.util.List)
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
	private final NullPrecedence nullPrecedence;
	private final int element;
	private final boolean ignoreCase;

	private Order(SortDirection order, NullPrecedence nullPrecedence, SingularAttribute<X, ?> attribute) {
		this.order = order;
		this.attribute = attribute;
		this.attributeName = attribute.getName();
		this.entityClass = attribute.getDeclaringType().getJavaType();
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = false;
	}

	private Order(SortDirection order, NullPrecedence nullPrecedence, SingularAttribute<X, ?> attribute, boolean ignoreCase) {
		this.order = order;
		this.attribute = attribute;
		this.attributeName = attribute.getName();
		this.entityClass = attribute.getDeclaringType().getJavaType();
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = ignoreCase;
	}

	private Order(SortDirection order, NullPrecedence nullPrecedence, Class<X> entityClass, String attributeName) {
		this.order = order;
		this.entityClass = entityClass;
		this.attributeName = attributeName;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = false;
	}

	private Order(SortDirection order, NullPrecedence nullPrecedence, int element) {
		this.order = order;
		this.entityClass = null;
		this.attributeName = null;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = element;
		this.ignoreCase = false;
	}

	private Order(SortDirection order, NullPrecedence nullPrecedence, Class<X> entityClass, String attributeName, boolean ignoreCase) {
		this.order = order;
		this.entityClass = entityClass;
		this.attributeName = attributeName;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = ignoreCase;
	}

	private Order(SortDirection order, NullPrecedence nullPrecedence, int element, boolean ignoreCase) {
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

	private Order(Order<X> other, NullPrecedence nullPrecedence) {
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
	 * with smaller values first. If the give attribute is of textual
	 * type, the ordering is case-sensitive.
	 */
	public static <T> Order<T> asc(SingularAttribute<T,?> attribute) {
		return new Order<>(ASCENDING, NullPrecedence.NONE, attribute);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * with larger values first. If the give attribute is of textual
	 * type, the ordering is case-sensitive.
	 */
	public static <T> Order<T> desc(SingularAttribute<T,?> attribute) {
		return new Order<>(DESCENDING, NullPrecedence.NONE, attribute);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * in the given direction. If the give attribute is of textual
	 * type, the ordering is case-sensitive.
	 */
	public static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction) {
		return new Order<>(direction, NullPrecedence.NONE, attribute);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * in the given direction, with the specified case-sensitivity.
	 */
	public static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction, boolean ignoreCase) {
		return new Order<>(direction, NullPrecedence.NONE, attribute, ignoreCase);
	}

	/**
	 * An order where an entity is sorted by the given attribute,
	 * in the given direction, with the specified precedence for
	 * null values. If the give attribute is of textual type, the
	 * ordering is case-sensitive.
	 */
	public static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction, NullPrecedence nullPrecedence) {
		return new Order<>(direction, nullPrecedence, attribute);
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, with smaller values first. If
	 * the named attribute is of textual type, the ordering is
	 * case-sensitive.
	 */
	public static <T> Order<T> asc(Class<T> entityClass, String attributeName) {
		return new Order<>( ASCENDING, NullPrecedence.NONE, entityClass, attributeName );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, with larger values first. If
	 * the named attribute is of textual type, the ordering is
	 * case-sensitive.
	 */
	public static <T> Order<T> desc(Class<T> entityClass, String attributeName) {
		return new Order<>( DESCENDING, NullPrecedence.NONE, entityClass, attributeName );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, in the given direction. If the
	 * named attribute is of textual type, the ordering is
	 * case-sensitive.
	 */
	public static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction) {
		return new Order<>( direction, NullPrecedence.NONE, entityClass, attributeName );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, in the given direction, with
	 * the specified case-sensitivity.
	 */
	public static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction, boolean ignoreCase) {
		return new Order<>( direction, NullPrecedence.NONE, entityClass, attributeName, ignoreCase );
	}

	/**
	 * An order where an entity of the given class is sorted by the
	 * attribute with the given name, in the given direction. If the
	 * named attribute is of textual type, with the specified
	 * precedence for null values. If the named attribute is of
	 * textual type, the ordering is case-sensitive.
	 */
	public static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction, NullPrecedence nullPrecedence) {
		return new Order<>( direction, nullPrecedence, entityClass, attributeName );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position with smaller values first. If the
	 * item is of textual type, the ordering is case-sensitive.
	 */
	public static Order<Object[]> asc(int element) {
		return new Order<>( ASCENDING, NullPrecedence.NONE, element );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position with larger values first. If the
	 * item is of textual type, the ordering is case-sensitive.
	 */
	public static Order<Object[]> desc(int element) {
		return new Order<>( DESCENDING, NullPrecedence.NONE, element );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position, in the given direction. If the item
	 * is of textual type, the ordering is case-sensitive.
	 */
	public static Order<Object[]> by(int element, SortDirection direction) {
		return new Order<>( direction, NullPrecedence.NONE, element );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position in the given direction, with the specified
	 * case-sensitivity.
	 */
	public static Order<Object[]> by(int element, SortDirection direction, boolean ignoreCase) {
		return new Order<>( direction, NullPrecedence.NONE, element, ignoreCase );
	}

	/**
	 * An order where the result set is sorted by the select item
	 * in the given position in the given direction, with the specified
	 * precedence for null values. If the named attribute is of
	 * textual type, the ordering is case-sensitive.
	 */
	public static Order<Object[]> by(int element, SortDirection direction, NullPrecedence nullPrecedence) {
		return new Order<>( direction, nullPrecedence, element );
	}

	public SortDirection getDirection() {
		return order;
	}

	public NullPrecedence getNullPrecedence() {
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
		return new Order<>( this, NullPrecedence.FIRST );
	}

	/**
	 * @return this order, but with nulls sorted last.
	 * @since 6.5
	 */
	public Order<X> withNullsLast() {
		return new Order<>( this, NullPrecedence.LAST );
	}

	@Override
	public String toString() {
		return attributeName + " " + order;
	}

	@Override
	public boolean equals(Object o) {
		if ( o instanceof Order) {
			Order<?> that = (Order<?>) o;
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
