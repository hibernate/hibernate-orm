/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.util.Collection;
import java.util.Map;

import org.hibernate.type.Type;

/**
 * The <tt>criterion</tt> package may be used by applications as a framework for building
 * new kinds of <tt>Criterion</tt>. However, it is intended that most applications will
 * simply use the built-in criterion types via the static factory methods of this class.
 *
 * See also the {@link Projections} factory methods for generating {@link Projection} instances
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see org.hibernate.Criteria
 */
public class Restrictions {
	/**
	 * Apply an "equal" constraint to the identifier property
	 *
	 * @param value The value to use in comparison
	 *
	 * @return Criterion
	 *
	 * @see IdentifierEqExpression
	 */
	public static Criterion idEq(Object value) {
		return new IdentifierEqExpression( value );
	}
	/**
	 * Apply an "equal" constraint to the named property
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return SimpleExpression
	 *
	 * @see SimpleExpression
	 */
	public static SimpleExpression eq(String propertyName, Object value) {
		return new SimpleExpression( propertyName, value, "=" );
	}

	/**
	 * Apply an "equal" constraint to the named property.  If the value
	 * is null, instead apply "is null".
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see #eq
	 * @see #isNull
	 */
	public static Criterion eqOrIsNull(String propertyName, Object value) {
		return value == null
				? isNull( propertyName )
				: eq( propertyName, value );
	}

	/**
	 * Apply a "not equal" constraint to the named property
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return The Criterion

	 * @see SimpleExpression
	 */
	public static SimpleExpression ne(String propertyName, Object value) {
		return new SimpleExpression( propertyName, value, "<>" );
	}

	/**
	 * Apply a "not equal" constraint to the named property.  If the value
	 * is null, instead apply "is not null".
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see #ne
	 * @see #isNotNull
	 */
	public static Criterion neOrIsNotNull(String propertyName, Object value) {
		return value == null
				? isNotNull( propertyName )
				: ne( propertyName, value );
	}

	/**
	 * Apply a "like" constraint to the named property
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SimpleExpression
	 */
	public static SimpleExpression like(String propertyName, Object value) {
		// todo : update this to use LikeExpression
		return new SimpleExpression( propertyName, value, " like " );
	}

	/**
	 * Apply a "like" constraint to the named property using the provided match mode
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 * @param matchMode The match mode to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SimpleExpression
	 */
	public static SimpleExpression like(String propertyName, String value, MatchMode matchMode) {
		// todo : update this to use LikeExpression
		return new SimpleExpression( propertyName, matchMode.toMatchString( value ), " like " );
	}

	/**
	 * A case-insensitive "like" (similar to Postgres <tt>ilike</tt> operator)
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see LikeExpression
	 */
	public static Criterion ilike(String propertyName, Object value) {
		if ( value == null ) {
			throw new IllegalArgumentException( "Comparison value passed to ilike cannot be null" );
		}
		return ilike( propertyName, value.toString(), MatchMode.EXACT );
	}

	/**
	 * A case-insensitive "like" (similar to Postgres <tt>ilike</tt> operator) using the provided match mode
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 * @param matchMode The match mode to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see LikeExpression
	 */
	public static Criterion ilike(String propertyName, String value, MatchMode matchMode) {
		if ( value == null ) {
			throw new IllegalArgumentException( "Comparison value passed to ilike cannot be null" );
		}
		return new LikeExpression( propertyName, value, matchMode, null, true );
	}

	/**
	 * Apply a "greater than" constraint to the named property
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SimpleExpression
	 */
	public static SimpleExpression gt(String propertyName, Object value) {
		return new SimpleExpression( propertyName, value, ">" );
	}

	/**
	 * Apply a "less than" constraint to the named property
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SimpleExpression
	 */
	public static SimpleExpression lt(String propertyName, Object value) {
		return new SimpleExpression( propertyName, value, "<" );
	}

	/**
	 * Apply a "less than or equal" constraint to the named property
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SimpleExpression
	 */
	public static SimpleExpression le(String propertyName, Object value) {
		return new SimpleExpression( propertyName, value, "<=" );
	}
	/**
	 * Apply a "greater than or equal" constraint to the named property
	 *
	 * @param propertyName The name of the property
	 * @param value The value to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SimpleExpression
	 */
	public static SimpleExpression ge(String propertyName, Object value) {
		return new SimpleExpression( propertyName, value, ">=" );
	}

	/**
	 * Apply a "between" constraint to the named property
	 *
	 * @param propertyName The name of the property
	 * @param lo The low value
	 * @param hi The high value
	 *
	 * @return The Criterion
	 *
	 * @see BetweenExpression
	 */
	public static Criterion between(String propertyName, Object lo, Object hi) {
		return new BetweenExpression( propertyName, lo, hi );
	}

	/**
	 * Apply an "in" constraint to the named property.
	 *
	 * @param propertyName The name of the property
	 * @param values The literal values to use in the IN restriction
	 *
	 * @return The Criterion
	 *
	 * @see InExpression
	 */
	public static Criterion in(String propertyName, Object... values) {
		return new InExpression( propertyName, values );
	}

	/**
	 * Apply an "in" constraint to the named property.
	 *
	 * @param propertyName The name of the property
	 * @param values The literal values to use in the IN restriction
	 *
	 * @return The Criterion
	 *
	 * @see InExpression
	 */
	public static Criterion in(String propertyName, Collection values) {
		return new InExpression( propertyName, values.toArray() );
	}

	/**
	 * Apply an "is null" constraint to the named property
	 *
	 * @param propertyName The name of the property
	 *
	 * @return Criterion
	 *
	 * @see NullExpression
	 */
	public static Criterion isNull(String propertyName) {
		return new NullExpression( propertyName );
	}

	/**
	 * Apply an "is not null" constraint to the named property
	 *
	 * @param propertyName The property name
	 *
	 * @return The Criterion
	 *
	 * @see NotNullExpression
	 */
	public static Criterion isNotNull(String propertyName) {
		return new NotNullExpression( propertyName );
	}

	/**
	 * Apply an "equal" constraint to two properties
	 *
	 * @param propertyName One property name
	 * @param otherPropertyName The other property name
	 *
	 * @return The Criterion
	 *
	 * @see PropertyExpression
	 */
	public static PropertyExpression eqProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression( propertyName, otherPropertyName, "=" );
	}

	/**
	 * Apply a "not equal" constraint to two properties
	 *
	 * @param propertyName One property name
	 * @param otherPropertyName The other property name
	 *
	 * @return The Criterion
	 *
	 * @see PropertyExpression
	 */
	public static PropertyExpression neProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression( propertyName, otherPropertyName, "<>" );
	}

	/**
	 * Apply a "less than" constraint to two properties
	 *
	 * @param propertyName One property name
	 * @param otherPropertyName The other property name
	 *
	 * @return The Criterion
	 *
	 * @see PropertyExpression
	 */
	public static PropertyExpression ltProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression( propertyName, otherPropertyName, "<" );
	}

	/**
	 * Apply a "less than or equal" constraint to two properties
	 *
	 * @param propertyName One property name
	 * @param otherPropertyName The other property name
	 *
	 * @return The Criterion
	 *
	 * @see PropertyExpression
	 */
	public static PropertyExpression leProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression( propertyName, otherPropertyName, "<=" );
	}

	/**
	 * Apply a "greater than" constraint to two properties
	 *
	 * @param propertyName One property name
	 * @param otherPropertyName The other property name
	 *
	 * @return The Criterion
	 *
	 * @see PropertyExpression
	 */
	public static PropertyExpression gtProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression( propertyName, otherPropertyName, ">" );
	}

	/**
	 * Apply a "greater than or equal" constraint to two properties
	 *
	 * @param propertyName One property name
	 * @param otherPropertyName The other property name
	 *
	 * @return The Criterion
	 *
	 * @see PropertyExpression
	 */
	public static PropertyExpression geProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression( propertyName, otherPropertyName, ">=" );
	}

	/**
	 * Return the conjuction of two expressions
	 *
	 * @param lhs One expression
	 * @param rhs The other expression
	 *
	 * @return The Criterion
	 */
	public static LogicalExpression and(Criterion lhs, Criterion rhs) {
		return new LogicalExpression(lhs, rhs, "and");
	}
	/**
	 * Return the conjuction of multiple expressions
	 *
	 * @param predicates The predicates making up the initial junction
	 *
	 * @return The conjunction
	 */
	public static Conjunction and(Criterion... predicates) {
		return conjunction( predicates );
	}

	/**
	 * Return the disjuction of two expressions
	 *
	 * @param lhs One expression
	 * @param rhs The other expression
	 *
	 * @return The Criterion
	 */
	public static LogicalExpression or(Criterion lhs, Criterion rhs) {
		return new LogicalExpression( lhs, rhs, "or" );
	}

	/**
	 * Return the disjuction of multiple expressions
	 *
	 * @param predicates The predicates making up the initial junction
	 *
	 * @return The conjunction
	 */
	public static Disjunction or(Criterion... predicates) {
		return disjunction( predicates );
	}

	/**
	 * Return the negation of an expression
	 *
	 * @param expression The expression to be negated
	 *
	 * @return Criterion
	 *
	 * @see NotExpression
	 */
	public static Criterion not(Criterion expression) {
		return new NotExpression( expression );
	}

	/**
	 * Create a restriction expressed in SQL with JDBC parameters.  Any occurrences of <tt>{alias}</tt> will be
	 * replaced by the table alias.
	 *
	 * @param sql The SQL restriction
	 * @param values The parameter values
	 * @param types The parameter types
	 *
	 * @return The Criterion
	 *
	 * @see SQLCriterion
	 */
	public static Criterion sqlRestriction(String sql, Object[] values, Type[] types) {
		return new SQLCriterion( sql, values, types );
	}

	/**
	 * Create a restriction expressed in SQL with one JDBC parameter.  Any occurrences of <tt>{alias}</tt> will be
	 * replaced by the table alias.
	 *
	 * @param sql The SQL restriction
	 * @param value The parameter value
	 * @param type The parameter type
	 *
	 * @return The Criterion
	 *
	 * @see SQLCriterion
	 */
	public static Criterion sqlRestriction(String sql, Object value, Type type) {
		return new SQLCriterion( sql, value, type );
	}

	/**
	 * Apply a constraint expressed in SQL with no JDBC parameters.  Any occurrences of <tt>{alias}</tt> will be
	 * replaced by the table alias.
	 *
	 * @param sql The SQL restriction
	 *
	 * @return The Criterion
	 *
	 * @see SQLCriterion
	 */
	public static Criterion sqlRestriction(String sql) {
		return new SQLCriterion( sql );
	}

	/**
	 * Group expressions together in a single conjunction (A and B and C...).
	 *
	 * This form creates an empty conjunction.  See {@link Conjunction#add(Criterion)}
	 *
	 * @return Conjunction
	 */
	public static Conjunction conjunction() {
		return new Conjunction();
	}

	/**
	 * Group expressions together in a single conjunction (A and B and C...).
	 *
	 * @param conditions The initial set of conditions to put into the Conjunction
	 *
	 * @return Conjunction
	 */
	public static Conjunction conjunction(Criterion... conditions) {
		return new Conjunction( conditions );
	}

	/**
	 * Group expressions together in a single disjunction (A or B or C...).
	 *
	 * This form creates an empty disjunction.  See {@link Disjunction#add(Criterion)}
	 *
	 * @return Conjunction
	 */
	public static Disjunction disjunction() {
		return new Disjunction();
	}

	/**
	 * Group expressions together in a single disjunction (A or B or C...).
	 *
	 * @param conditions The initial set of conditions to put into the Disjunction
	 *
	 * @return Conjunction
	 */
	public static Disjunction disjunction(Criterion... conditions) {
		return new Disjunction( conditions );
	}

	/**
	 * Apply an "equals" constraint to each property in the key set of a <tt>Map</tt>
	 *
	 * @param propertyNameValues a map from property names to values
	 *
	 * @return Criterion
	 *
	 * @see Conjunction
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static Criterion allEq(Map<String,?> propertyNameValues) {
		final Conjunction conj = conjunction();

		for ( Map.Entry<String,?> entry : propertyNameValues.entrySet() ) {
			conj.add( eq( entry.getKey(), entry.getValue() ) );
		}
		return conj;
	}

	/**
	 * Constrain a collection valued property to be empty
	 *
	 * @param propertyName The name of the collection property
	 *
	 * @return The Criterion
	 *
	 * @see EmptyExpression
	 */
	public static Criterion isEmpty(String propertyName) {
		return new EmptyExpression( propertyName );
	}

	/**
	 * Constrain a collection valued property to be non-empty
	 *
	 * @param propertyName The name of the collection property
	 *
	 * @return The Criterion
	 *
	 * @see NotEmptyExpression
	 */
	public static Criterion isNotEmpty(String propertyName) {
		return new NotEmptyExpression( propertyName );
	}

	/**
	 * Constrain a collection valued property by size
	 *
	 * @param propertyName The name of the collection property
	 * @param size The size to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SizeExpression
	 */
	public static Criterion sizeEq(String propertyName, int size) {
		return new SizeExpression( propertyName, size, "=" );
	}

	/**
	 * Constrain a collection valued property by size
	 *
	 * @param propertyName The name of the collection property
	 * @param size The size to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SizeExpression
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static Criterion sizeNe(String propertyName, int size) {
		return new SizeExpression( propertyName, size, "<>" );
	}

	/**
	 * Constrain a collection valued property by size
	 *
	 * @param propertyName The name of the collection property
	 * @param size The size to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SizeExpression
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static Criterion sizeGt(String propertyName, int size) {
		return new SizeExpression( propertyName, size, "<" );
	}

	/**
	 * Constrain a collection valued property by size
	 *
	 * @param propertyName The name of the collection property
	 * @param size The size to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SizeExpression
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static Criterion sizeLt(String propertyName, int size) {
		return new SizeExpression( propertyName, size, ">" );
	}

	/**
	 * Constrain a collection valued property by size
	 *
	 * @param propertyName The name of the collection property
	 * @param size The size to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SizeExpression
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static Criterion sizeGe(String propertyName, int size) {
		return new SizeExpression( propertyName, size, "<=" );
	}

	/**
	 * Constrain a collection valued property by size
	 *
	 * @param propertyName The name of the collection property
	 * @param size The size to use in comparison
	 *
	 * @return The Criterion
	 *
	 * @see SizeExpression
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static Criterion sizeLe(String propertyName, int size) {
		return new SizeExpression( propertyName, size, ">=" );
	}

	/**
	 * Consider using any of the natural id based loading stuff from session instead, especially in cases
	 * where the restriction is the full set of natural id values.
	 *
	 * @return The Criterion
	 *
	 * @see NaturalIdentifier
	 *
	 * @see org.hibernate.Session#byNaturalId(Class)
	 * @see org.hibernate.Session#byNaturalId(String)
	 * @see org.hibernate.Session#bySimpleNaturalId(Class)
	 * @see org.hibernate.Session#bySimpleNaturalId(String)
	 */
	public static NaturalIdentifier naturalId() {
		return new NaturalIdentifier();
	}

	protected Restrictions() {
		// cannot be instantiated, but needs to be protected so Expression can extend it
	}

}
