/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.criterion;

/**
 * Factory class for criterion instances that represent expressions
 * involving subqueries.
 * 
 * @see Restrictions
 * @see Projection
 * @see org.hibernate.Criteria
 *
 * @author Gavin King
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings( {"UnusedDeclaration"})
public class Subqueries {

	/**
	 * Creates a criterion which checks for the existence of rows in the subquery result
	 *
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see ExistsSubqueryExpression
	 */
	public static Criterion exists(DetachedCriteria dc) {
		return new ExistsSubqueryExpression( "exists", dc );
	}

	/**
	 * Creates a criterion which checks for the non-existence of rows in the subquery result
	 *
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see ExistsSubqueryExpression
	 */
	public static Criterion notExists(DetachedCriteria dc) {
		return new ExistsSubqueryExpression( "not exists", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property equals ALL the values in the
	 * subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyEqAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "=", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is greater-than ALL the values in the
	 * subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyGtAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, ">", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is less-than ALL the values in the
	 * subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyLtAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "<", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is greater-than-or-equal-to ALL the
	 * values in the subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyGeAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, ">=", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is less-than-or-equal-to ALL the
	 * values in the subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyLeAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "<=", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is greater-than SOME of the
	 * values in the subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyGtSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, ">", "some", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is less-than SOME of the
	 * values in the subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyLtSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "<", "some", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is greater-than-or-equal-to SOME of the
	 * values in the subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyGeSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, ">=", "some", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is less-than-or-equal-to SOME of the
	 * values in the subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyLeSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "<=", "some", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is in the set of values in the
	 * subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyIn(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "in", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is not-in the set of values in
	 * the subquery result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyNotIn(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "not in", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property as being equal to the set of values in
	 * the subquery result.  The implication is that the subquery returns a single result..
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyEq(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "=", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is not equal to the value in the
	 * subquery result.  The assumption is that the subquery returns a single result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 */
	public static Criterion propertyNe(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<>", null, dc);
	}

	/**
	 * Creates a criterion which checks that the value of a given property is greater-than the value in the
	 * subquery result.  The assumption is that the subquery returns a single result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 * @see #propertyGtAll
	 * @see #propertyGtSome
	 */
	public static Criterion propertyGt(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, ">", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is less-than the value in the
	 * subquery result.  The assumption is that the subquery returns a single result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 * @see #propertyLtAll
	 * @see #propertyLtSome
	 */
	public static Criterion propertyLt(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "<", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is greater-than-or-equal-to the value
	 * in the subquery result.  The assumption is that the subquery returns a single result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 * @see #propertyGeAll
	 * @see #propertyGeSome
	 */
	public static Criterion propertyGe(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, ">=", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given property is less-than-or-equal-to the value
	 * in the subquery result.  The assumption is that the subquery returns a single result.
	 *
	 * @param propertyName The name of the property to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertySubqueryExpression
	 * @see #propertyLeAll
	 * @see #propertyLeSome
	 */
	public static Criterion propertyLe(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression( propertyName, "<=", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of multiple given properties as being equal to the set of
	 * values in the subquery result.  The implication is that the subquery returns a single result.  This form is
	 * however implicitly using tuple comparisons
	 *
	 * @param propertyNames The names of the properties to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertiesSubqueryExpression
	 */
	public static Criterion propertiesEq(String[] propertyNames, DetachedCriteria dc) {
		return new PropertiesSubqueryExpression( propertyNames, "=", dc );
	}

	/**
	 * Creates a criterion which checks that the value of multiple given properties as being not-equal to the set of
	 * values in the subquery result.  The assumption is that the subquery returns a single result.  This form is
	 * however implicitly using tuple comparisons
	 *
	 * @param propertyNames The names of the properties to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertiesSubqueryExpression
	 */
	public static Criterion propertiesNotEq(String[] propertyNames, DetachedCriteria dc) {
		return new PropertiesSubqueryExpression( propertyNames, "<>", dc );
	}

	/**
	 * Creates a criterion which checks that the value of multiple given properties as being in to the set of
	 * values in the subquery result.  This form is implicitly using tuple comparisons
	 *
	 * @param propertyNames The names of the properties to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertiesSubqueryExpression
	 */
	public static Criterion propertiesIn(String[] propertyNames, DetachedCriteria dc) {
		return new PropertiesSubqueryExpression( propertyNames, "in", dc );
	}

	/**
	 * Creates a criterion which checks that the value of multiple given properties as being not-in to the set of
	 * values in the subquery result.  This form is implicitly using tuple comparisons
	 *
	 * @param propertyNames The names of the properties to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see PropertiesSubqueryExpression
	 */
	public static Criterion propertiesNotIn(String[] propertyNames, DetachedCriteria dc) {
		return new PropertiesSubqueryExpression( propertyNames, "not in", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal equals ALL the values in the
	 * subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion eqAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "=", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is greater-than ALL the values in the
	 * subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion gtAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, ">", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is less-than ALL the values in the
	 * subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion ltAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "<", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is greater-than-or-equal-to ALL the values in the
	 * subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion geAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, ">=", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is less-than-or-equal-to ALL the values in the
	 * subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion leAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "<=", "all", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is greater-than SOME of the values in the
	 * subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion gtSome(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, ">", "some", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is less-than SOME of the values in the
	 * subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion ltSome(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "<", "some", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is greater-than-or-equal-to SOME of the values
	 * in the subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion geSome(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, ">=", "some", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is less-than-or-equal-to SOME of the values
	 * in the subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion leSome(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "<=", "some", dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is IN the values in the
	 * subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion in(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "in", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a literal is NOT IN the values in the
	 * subquery result.
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion notIn(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "not in", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given literal as being equal to the value in
	 * the subquery result.  The implication is that the subquery returns a single result..
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion eq(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "=", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given literal as being not-equal to the value in
	 * the subquery result.  The implication is that the subquery returns a single result..
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion ne(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<>", null, dc);
	}

	/**
	 * Creates a criterion which checks that the value of a given literal as being greater-than the value in
	 * the subquery result.  The implication is that the subquery returns a single result..
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion gt(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, ">", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given literal as being less-than the value in
	 * the subquery result.  The implication is that the subquery returns a single result..
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion lt(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "<", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given literal as being greater-than-or-equal-to the
	 * value in the subquery result.  The implication is that the subquery returns a single result..
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion ge(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, ">=", null, dc );
	}

	/**
	 * Creates a criterion which checks that the value of a given literal as being less-than-or-equal-to the
	 * value in the subquery result.  The implication is that the subquery returns a single result..
	 *
	 * @param value The literal value to use in comparison
	 * @param dc The detached criteria representing the subquery
	 *
	 * @return The Criterion
	 *
	 * @see SimpleSubqueryExpression
	 */
	public static Criterion le(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression( value, "<=", null, dc );
	}

	private Subqueries() {
	}
}
