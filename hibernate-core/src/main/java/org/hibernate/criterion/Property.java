/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.util.Collection;

/**
 * A factory for property-specific criterion and projection instances
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Property extends PropertyProjection {
	/**
	 * Factory for Property instances.
	 *
	 * @param propertyName The name of the property.
	 *
	 * @return The Property instance
	 */
	public static Property forName(String propertyName) {
		return new Property( propertyName );
	}

	/**
	 * Constructs a Property.  non-private to allow subclassing.
	 *
	 * @param propertyName The property name.
	 */
	protected Property(String propertyName) {
		super( propertyName );
	}

	/**
	 * Creates a BETWEEN restriction for this property between the given min and max
	 *
	 * @param min The minimum
	 * @param max The maximum
	 *
	 * @return The BETWEEN restriction
	 *
	 * @see Restrictions#between(String, Object, Object)
	 */
	public Criterion between(Object min, Object max) {
		return Restrictions.between( getPropertyName(), min, max );
	}

	/**
	 * Creates an IN restriction for this property based on the given list of literals
	 *
	 * @param values The literal values
	 *
	 * @return The IN restriction
	 *
	 * @see Restrictions#in(String, Collection)
	 */
	public Criterion in(Collection values) {
		return Restrictions.in( getPropertyName(), values );
	}

	/**
	 * Creates an IN restriction for this property based on the given list of literals
	 *
	 * @param values The literal values
	 *
	 * @return The IN restriction
	 *
	 * @see Restrictions#in(String, Object[])
	 */
	public Criterion in(Object... values) {
		return Restrictions.in( getPropertyName(), values );
	}

	/**
	 * Creates a LIKE restriction for this property
	 *
	 * @param value The value to like compare with
	 *
	 * @return The LIKE restriction
	 *
	 * @see Restrictions#like(String, Object)
	 */
	public SimpleExpression like(Object value) {
		return Restrictions.like( getPropertyName(), value );
	}

	/**
	 * Creates a LIKE restriction for this property
	 *
	 * @param value The value to like compare with
	 * @param matchMode The match mode to apply to the LIKE
	 *
	 * @return The LIKE restriction
	 *
	 * @see Restrictions#like(String, String, MatchMode)
	 */
	public SimpleExpression like(String value, MatchMode matchMode) {
		return Restrictions.like( getPropertyName(), value, matchMode );
	}

	/**
	 * Creates an equality restriction.
	 *
	 * @param value The value to check against
	 *
	 * @return The equality restriction.
	 *
	 * @see Restrictions#eq(String, Object)
	 */
	public SimpleExpression eq(Object value) {
		return Restrictions.eq( getPropertyName(), value );
	}

	/**
	 * Creates an equality restriction capable of also rendering as IS NULL if the given value is {@code null}
	 *
	 * @param value The value to check against
	 *
	 * @return The equality restriction.
	 *
	 * @see Restrictions#eqOrIsNull(String, Object)
	 * @see #eq
	 * @see #isNull
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion eqOrIsNull(Object value) {
		return Restrictions.eqOrIsNull( getPropertyName(), value );
	}

	/**
	 * Creates an non-equality restriction.
	 *
	 * @param value The value to check against
	 *
	 * @return The non-equality restriction.
	 *
	 * @see Restrictions#ne(String, Object)
	 */
	public SimpleExpression ne(Object value) {
		return Restrictions.ne( getPropertyName(), value );
	}

	/**
	 * Creates an non-equality restriction capable of also rendering as IS NOT NULL if the given value is {@code null}
	 *
	 * @param value The value to check against
	 *
	 * @return The non-equality restriction.
	 *
	 * @see Restrictions#neOrIsNotNull(String, Object)
	 * @see #ne
	 * @see #isNotNull
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion neOrIsNotNull(Object value) {
		return Restrictions.neOrIsNotNull( getPropertyName(), value );
	}

	/**
	 * Create a greater-than restriction based on this property
	 *
	 * @param value The value to check against
	 *
	 * @return The greater-than restriction
	 *
	 * @see Restrictions#gt(String, Object)
	 */
	public SimpleExpression gt(Object value) {
		return Restrictions.gt( getPropertyName(), value );
	}

	/**
	 * Create a less-than restriction based on this property
	 *
	 * @param value The value to check against
	 *
	 * @return The less-than restriction
	 *
	 * @see Restrictions#lt(String, Object)
	 */
	public SimpleExpression lt(Object value) {
		return Restrictions.lt( getPropertyName(), value );
	}

	/**
	 * Create a less-than-or-equal-to restriction based on this property
	 *
	 * @param value The value to check against
	 *
	 * @return The less-than-or-equal-to restriction
	 *
	 * @see Restrictions#le(String, Object)
	 */
	public SimpleExpression le(Object value) {
		return Restrictions.le( getPropertyName(), value );
	}

	/**
	 * Create a greater-than-or-equal-to restriction based on this property
	 *
	 * @param value The value to check against
	 *
	 * @return The greater-than-or-equal-to restriction
	 *
	 * @see Restrictions#ge(String, Object)
	 */
	public SimpleExpression ge(Object value) {
		return Restrictions.ge( getPropertyName(), value );
	}

	/**
	 * Creates an equality restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#eqProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression eqProperty(Property other) {
		return Restrictions.eqProperty( getPropertyName(), other.getPropertyName() );
	}

	/**
	 * Creates an equality restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#eqProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression eqProperty(String other) {
		return Restrictions.eqProperty( getPropertyName(), other );
	}

	/**
	 * Creates an non-equality restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#neProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression neProperty(Property other) {
		return Restrictions.neProperty( getPropertyName(), other.getPropertyName() );
	}

	/**
	 * Creates an non-equality restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#neProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression neProperty(String other) {
		return Restrictions.neProperty( getPropertyName(), other );
	}

	/**
	 * Creates an less-than-or-equal-to restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#leProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression leProperty(Property other) {
		return Restrictions.leProperty( getPropertyName(), other.getPropertyName() );
	}

	/**
	 * Creates an less-than-or-equal-to restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#leProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression leProperty(String other) {
		return Restrictions.leProperty( getPropertyName(), other );
	}

	/**
	 * Creates an greater-than-or-equal-to restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#geProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression geProperty(Property other) {
		return Restrictions.geProperty( getPropertyName(), other.getPropertyName() );
	}

	/**
	 * Creates an greater-than-or-equal-to restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#geProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression geProperty(String other) {
		return Restrictions.geProperty( getPropertyName(), other );
	}

	/**
	 * Creates an less-than restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#ltProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression ltProperty(Property other) {
		return Restrictions.ltProperty( getPropertyName(), other.getPropertyName() );
	}

	/**
	 * Creates an less-than restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#ltProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression ltProperty(String other) {
		return Restrictions.ltProperty( getPropertyName(), other );
	}

	/**
	 * Creates an greater-than restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#geProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression gtProperty(Property other) {
		return Restrictions.gtProperty( getPropertyName(), other.getPropertyName() );
	}

	/**
	 * Creates an greater-than restriction between 2 properties
	 *
	 * @param other The other property to compare against
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#geProperty(String, String)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PropertyExpression gtProperty(String other) {
		return Restrictions.gtProperty( getPropertyName(), other );
	}

	/**
	 * Creates a NULL restriction
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#isNull(String)
	 */
	public Criterion isNull() {
		return Restrictions.isNull( getPropertyName() );
	}

	/**
	 * Creates a NOT NULL restriction
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#isNotNull(String)
	 */
	public Criterion isNotNull() {
		return Restrictions.isNotNull( getPropertyName() );
	}

	/**
	 * Creates a restriction to check that a collection is empty
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#isEmpty(String)
	 */
	public Criterion isEmpty() {
		return Restrictions.isEmpty( getPropertyName() );
	}

	/**
	 * Creates a restriction to check that a collection is not empty
	 *
	 * @return The restriction
	 *
	 * @see Restrictions#isNotEmpty(String)
	 */
	public Criterion isNotEmpty() {
		return Restrictions.isNotEmpty( getPropertyName() );
	}

	/**
	 * Creates a property count projection
	 *
	 * @return The projection
	 *
	 * @see Projections#count
	 */
	public CountProjection count() {
		return Projections.count( getPropertyName() );
	}

	/**
	 * Creates a property max projection
	 *
	 * @return The projection
	 *
	 * @see Projections#max
	 */
	public AggregateProjection max() {
		return Projections.max( getPropertyName() );
	}

	/**
	 * Creates a property min projection
	 *
	 * @return The projection
	 *
	 * @see Projections#min
	 */
	public AggregateProjection min() {
		return Projections.min( getPropertyName() );
	}

	/**
	 * Creates a property avg projection
	 *
	 * @return The projection
	 *
	 * @see Projections#avg
	 */
	public AggregateProjection avg() {
		return Projections.avg( getPropertyName() );
	}

	/**
	 * Creates a projection for this property as a group expression
	 *
	 * @return The group projection
	 *
	 * @see Projections#groupProperty
	 */
	public PropertyProjection group() {
		return Projections.groupProperty( getPropertyName() );
	}

	/**
	 * Creates an ascending ordering for this property
	 *
	 * @return The order
	 */
	public Order asc() {
		return Order.asc( getPropertyName() );
	}

	/**
	 * Creates a descending ordering for this property
	 *
	 * @return The order
	 */
	public Order desc() {
		return Order.desc( getPropertyName() );
	}
	
	/**
	 * Get a component attribute of this property.
	 *
	 * @param propertyName The sub property name
	 *
	 * @return The property
	 */
	public Property getProperty(String propertyName) {
		return forName( getPropertyName() + '.' + propertyName );
	}

	/**
	 * Creates a sub-query equality expression for this property
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyEq(String, DetachedCriteria)
	 */
	public Criterion eq(DetachedCriteria subselect) {
		return Subqueries.propertyEq( getPropertyName(), subselect );
	}

	/**
	 * Creates a sub-query non-equality expression for this property
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyNe(String, DetachedCriteria)
	 */
	public Criterion ne(DetachedCriteria subselect) {
		return Subqueries.propertyNe( getPropertyName(), subselect );
	}

	/**
	 * Creates a sub-query less-than expression for this property
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyLt(String, DetachedCriteria)
	 */
	public Criterion lt(DetachedCriteria subselect) {
		return Subqueries.propertyLt( getPropertyName(), subselect );
	}

	/**
	 * Creates a sub-query less-than-or-equal-to expression for this property
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyLe(String, DetachedCriteria)
	 */
	public Criterion le(DetachedCriteria subselect) {
		return Subqueries.propertyLe( getPropertyName(), subselect );
	}

	/**
	 * Creates a sub-query greater-than expression for this property
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyGt(String, DetachedCriteria)
	 */
	public Criterion gt(DetachedCriteria subselect) {
		return Subqueries.propertyGt( getPropertyName(), subselect );
	}

	/**
	 * Creates a sub-query greater-than-or-equal-to expression for this property
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyGe(String, DetachedCriteria)
	 */
	public Criterion ge(DetachedCriteria subselect) {
		return Subqueries.propertyGe( getPropertyName(), subselect );
	}

	/**
	 * Creates a sub-query NOT IN expression for this property.  I.e., {@code [prop] NOT IN [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyNotIn(String, DetachedCriteria)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion notIn(DetachedCriteria subselect) {
		return Subqueries.propertyNotIn( getPropertyName(), subselect );
	}

	/**
	 * Creates a sub-query IN expression for this property.  I.e., {@code [prop] IN [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyIn(String, DetachedCriteria)
	 */
	public Criterion in(DetachedCriteria subselect) {
		return Subqueries.propertyIn( getPropertyName(), subselect );
	}

	/**
	 * Creates a equals-all sub-query expression for this property.  I.e., {@code [prop] = ALL [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyEqAll(String, DetachedCriteria)
	 */
	public Criterion eqAll(DetachedCriteria subselect) {
		return Subqueries.propertyEqAll( getPropertyName(), subselect );
	}

	/**
	 * Creates a greater-than-all sub-query expression for this property.  I.e., {@code [prop] > ALL [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyGtAll(String, DetachedCriteria)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion gtAll(DetachedCriteria subselect) {
		return Subqueries.propertyGtAll( getPropertyName(), subselect );
	}

	/**
	 * Creates a less-than-all sub-query expression for this property.  I.e., {@code [prop] < ALL [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyLtAll(String, DetachedCriteria)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion ltAll(DetachedCriteria subselect) {
		return Subqueries.propertyLtAll( getPropertyName(), subselect );
	}

	/**
	 * Creates a less-than-or-equal-to-all sub-query expression for this property.  I.e., {@code [prop] <= ALL [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyLeAll(String, DetachedCriteria)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion leAll(DetachedCriteria subselect) {
		return Subqueries.propertyLeAll( getPropertyName(), subselect );
	}

	/**
	 * Creates a greater-than-or-equal-to-all sub-query expression for this property.  I.e., {@code [prop] >= ALL [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyGeAll(String, DetachedCriteria)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion geAll(DetachedCriteria subselect) {
		return Subqueries.propertyGeAll( getPropertyName(), subselect );
	}

	/**
	 * Creates a greater-than-some sub-query expression for this property.  I.e., {@code [prop] > SOME [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyGtSome(String, DetachedCriteria)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion gtSome(DetachedCriteria subselect) {
		return Subqueries.propertyGtSome( getPropertyName(), subselect );
	}

	/**
	 * Creates a less-than-some sub-query expression for this property.  I.e., {@code [prop] < SOME [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyLtSome(String, DetachedCriteria)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion ltSome(DetachedCriteria subselect) {
		return Subqueries.propertyLtSome( getPropertyName(), subselect );
	}

	/**
	 * Creates a less-than-or-equal-to-some sub-query expression for this property.  I.e., {@code [prop] <= SOME [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyLeSome(String, DetachedCriteria)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion leSome(DetachedCriteria subselect) {
		return Subqueries.propertyLeSome( getPropertyName(), subselect );
	}

	/**
	 * Creates a greater-than-or-equal-to-some sub-query expression for this property.  I.e., {@code [prop] >= SOME [subquery]}
	 *
	 * @param subselect The sub-query
	 *
	 * @return The expression
	 *
	 * @see Subqueries#propertyGeSome(String, DetachedCriteria)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Criterion geSome(DetachedCriteria subselect) {
		return Subqueries.propertyGeSome( getPropertyName(), subselect );
	}

}
