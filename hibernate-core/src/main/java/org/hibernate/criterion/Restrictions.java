/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.criterion;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.Type;

/**
 * The <tt>criterion</tt> package may be used by applications as a framework for building
 * new kinds of <tt>Criterion</tt>. However, it is intended that most applications will
 * simply use the built-in criterion types via the static factory methods of this class.
 *
 * @see org.hibernate.Criteria
 * @see Projections factory methods for <tt>Projection</tt> instances
 * @author Gavin King
 */
public class Restrictions {

	Restrictions() {
		//cannot be instantiated
	}

	/**
	 * Apply an "equal" constraint to the identifier property
	 * @param value
	 * @return Criterion
	 */
	public static Criterion idEq(Object value) {
		return new IdentifierEqExpression(value);
	}
	/**
	 * Apply an "equal" constraint to the named property
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static SimpleExpression eq(String propertyName, Object value) {
		return new SimpleExpression(propertyName, value, "=");
	}
	/**
	 * Apply a "not equal" constraint to the named property
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static SimpleExpression ne(String propertyName, Object value) {
		return new SimpleExpression(propertyName, value, "<>");
	}
	/**
	 * Apply a "like" constraint to the named property
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static SimpleExpression like(String propertyName, Object value) {
		return new SimpleExpression(propertyName, value, " like ");
	}
	/**
	 * Apply a "like" constraint to the named property
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static SimpleExpression like(String propertyName, String value, MatchMode matchMode) {
		return new SimpleExpression(propertyName, matchMode.toMatchString(value), " like " );
	}

	/**
	 * A case-insensitive "like", similar to Postgres <tt>ilike</tt>
	 * operator
	 *
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static Criterion ilike(String propertyName, String value, MatchMode matchMode) {
		return new LikeExpression(propertyName, value, matchMode, null, true);
	}
	/**
	 * A case-insensitive "like", similar to Postgres <tt>ilike</tt>
	 * operator
	 *
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static Criterion ilike(String propertyName, Object value) {
		if ( value == null ) {
			throw new IllegalArgumentException( "Comparison value passed to ilike cannot be null" );
		}
		return ilike( propertyName, value.toString(), MatchMode.EXACT );
	}

	/**
	 * Apply a "greater than" constraint to the named property
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static SimpleExpression gt(String propertyName, Object value) {
		return new SimpleExpression(propertyName, value, ">");
	}
	/**
	 * Apply a "less than" constraint to the named property
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static SimpleExpression lt(String propertyName, Object value) {
		return new SimpleExpression(propertyName, value, "<");
	}
	/**
	 * Apply a "less than or equal" constraint to the named property
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static SimpleExpression le(String propertyName, Object value) {
		return new SimpleExpression(propertyName, value, "<=");
	}
	/**
	 * Apply a "greater than or equal" constraint to the named property
	 * @param propertyName
	 * @param value
	 * @return Criterion
	 */
	public static SimpleExpression ge(String propertyName, Object value) {
		return new SimpleExpression(propertyName, value, ">=");
	}
	/**
	 * Apply a "between" constraint to the named property
	 * @param propertyName
	 * @param lo value
	 * @param hi value
	 * @return Criterion
	 */
	public static Criterion between(String propertyName, Object lo, Object hi) {
		return new BetweenExpression(propertyName, lo, hi);
	}
	/**
	 * Apply an "in" constraint to the named property
	 * @param propertyName
	 * @param values
	 * @return Criterion
	 */
	public static Criterion in(String propertyName, Object[] values) {
		return new InExpression(propertyName, values);
	}
	/**
	 * Apply an "in" constraint to the named property
	 * @param propertyName
	 * @param values
	 * @return Criterion
	 */
	public static Criterion in(String propertyName, Collection values) {
		return new InExpression( propertyName, values.toArray() );
	}
	/**
	 * Apply an "is null" constraint to the named property
	 * @return Criterion
	 */
	public static Criterion isNull(String propertyName) {
		return new NullExpression(propertyName);
	}
	/**
	 * Apply an "equal" constraint to two properties
	 */
	public static PropertyExpression eqProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression(propertyName, otherPropertyName, "=");
	}
	/**
	 * Apply a "not equal" constraint to two properties
	 */
	public static PropertyExpression neProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression(propertyName, otherPropertyName, "<>");
	}
	/**
	 * Apply a "less than" constraint to two properties
	 */
	public static PropertyExpression ltProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression(propertyName, otherPropertyName, "<");
	}
	/**
	 * Apply a "less than or equal" constraint to two properties
	 */
	public static PropertyExpression leProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression(propertyName, otherPropertyName, "<=");
	}
	/**
	 * Apply a "greater than" constraint to two properties
	 */
	public static PropertyExpression gtProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression(propertyName, otherPropertyName, ">");
	}
	/**
	 * Apply a "greater than or equal" constraint to two properties
	 */
	public static PropertyExpression geProperty(String propertyName, String otherPropertyName) {
		return new PropertyExpression(propertyName, otherPropertyName, ">=");
	}
	/**
	 * Apply an "is not null" constraint to the named property
	 * @return Criterion
	 */
	public static Criterion isNotNull(String propertyName) {
		return new NotNullExpression(propertyName);
	}
	/**
	 * Return the conjuction of two expressions
	 *
	 * @param lhs
	 * @param rhs
	 * @return Criterion
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
		Conjunction conjunction = conjunction();
		if ( predicates != null ) {
			for ( Criterion predicate : predicates ) {
				conjunction.add( predicate );
			}
		}
		return conjunction;
	}
	/**
	 * Return the disjuction of two expressions
	 *
	 * @param lhs
	 * @param rhs
	 * @return Criterion
	 */
	public static LogicalExpression or(Criterion lhs, Criterion rhs) {
		return new LogicalExpression(lhs, rhs, "or");
	}
	/**
	 * Return the disjuction of multiple expressions
	 *
	 * @param predicates The predicates making up the initial junction
	 *
	 * @return The conjunction
	 */
	public static Disjunction or(Criterion... predicates) {
		Disjunction disjunction = disjunction();
		if ( predicates != null ) {
			for ( Criterion predicate : predicates ) {
				disjunction.add( predicate );
			}
		}
		return disjunction;
	}
	/**
	 * Return the negation of an expression
	 *
	 * @param expression
	 * @return Criterion
	 */
	public static Criterion not(Criterion expression) {
		return new NotExpression(expression);
	}
	/**
	 * Apply a constraint expressed in SQL, with the given JDBC
	 * parameters. Any occurrences of <tt>{alias}</tt> will be
	 * replaced by the table alias.
	 *
	 * @param sql
	 * @param values
	 * @param types
	 * @return Criterion
	 */
	public static Criterion sqlRestriction(String sql, Object[] values, Type[] types) {
		return new SQLCriterion(sql, values, types);
	}
	/**
	 * Apply a constraint expressed in SQL, with the given JDBC
	 * parameter. Any occurrences of <tt>{alias}</tt> will be replaced
	 * by the table alias.
	 *
	 * @param sql
	 * @param value
	 * @param type
	 * @return Criterion
	 */
	public static Criterion sqlRestriction(String sql, Object value, Type type) {
		return new SQLCriterion(sql, new Object[] { value }, new Type[] { type } );
	}
	/**
	 * Apply a constraint expressed in SQL. Any occurrences of <tt>{alias}</tt>
	 * will be replaced by the table alias.
	 *
	 * @param sql
	 * @return Criterion
	 */
	public static Criterion sqlRestriction(String sql) {
		return new SQLCriterion(sql, ArrayHelper.EMPTY_OBJECT_ARRAY, ArrayHelper.EMPTY_TYPE_ARRAY);
	}

	/**
	 * Group expressions together in a single conjunction (A and B and C...)
	 *
	 * @return Conjunction
	 */
	public static Conjunction conjunction() {
		return new Conjunction();
	}

	/**
	 * Group expressions together in a single disjunction (A or B or C...)
	 *
	 * @return Conjunction
	 */
	public static Disjunction disjunction() {
		return new Disjunction();
	}

	/**
	 * Apply an "equals" constraint to each property in the
	 * key set of a <tt>Map</tt>
	 *
	 * @param propertyNameValues a map from property names to values
	 * @return Criterion
	 */
	public static Criterion allEq(Map propertyNameValues) {
		Conjunction conj = conjunction();
		Iterator iter = propertyNameValues.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			conj.add( eq( (String) me.getKey(), me.getValue() ) );
		}
		return conj;
	}

	/**
	 * Constrain a collection valued property to be empty
	 */
	public static Criterion isEmpty(String propertyName) {
		return new EmptyExpression(propertyName);
	}

	/**
	 * Constrain a collection valued property to be non-empty
	 */
	public static Criterion isNotEmpty(String propertyName) {
		return new NotEmptyExpression(propertyName);
	}

	/**
	 * Constrain a collection valued property by size
	 */
	public static Criterion sizeEq(String propertyName, int size) {
		return new SizeExpression(propertyName, size, "=");
	}

	/**
	 * Constrain a collection valued property by size
	 */
	public static Criterion sizeNe(String propertyName, int size) {
		return new SizeExpression(propertyName, size, "<>");
	}

	/**
	 * Constrain a collection valued property by size
	 */
	public static Criterion sizeGt(String propertyName, int size) {
		return new SizeExpression(propertyName, size, "<");
	}

	/**
	 * Constrain a collection valued property by size
	 */
	public static Criterion sizeLt(String propertyName, int size) {
		return new SizeExpression(propertyName, size, ">");
	}

	/**
	 * Constrain a collection valued property by size
	 */
	public static Criterion sizeGe(String propertyName, int size) {
		return new SizeExpression(propertyName, size, "<=");
	}

	/**
	 * Constrain a collection valued property by size
	 */
	public static Criterion sizeLe(String propertyName, int size) {
		return new SizeExpression(propertyName, size, ">=");
	}

	/**
	 * Consider using any of the natural id based loading stuff from session instead, especially in cases
	 * where the restriction is the full set of natural id values.
	 *
	 * @see Session#byNaturalId(Class)
	 * @see Session#byNaturalId(String)
	 * @see Session#bySimpleNaturalId(Class)
	 * @see Session#bySimpleNaturalId(String)
	 */
	public static NaturalIdentifier naturalId() {
		return new NaturalIdentifier();
	}

}
