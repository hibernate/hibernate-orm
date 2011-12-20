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
		
	public static Criterion exists(DetachedCriteria dc) {
		return new ExistsSubqueryExpression("exists", dc);
	}
	
	public static Criterion notExists(DetachedCriteria dc) {
		return new ExistsSubqueryExpression("not exists", dc);
	}
	
	public static Criterion propertyEqAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "=", "all", dc);
	}
	
	public static Criterion propertyIn(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "in", null, dc);
	}
	
	public static Criterion propertyNotIn(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "not in", null, dc);
	}
	
	public static Criterion propertyEq(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "=", null, dc);
	}

	public static Criterion propertiesEq(String[] propertyNames, DetachedCriteria dc) {
		return new PropertiesSubqueryExpression(propertyNames, "=", dc);
	}

	public static Criterion propertiesNotEq(String[] propertyNames, DetachedCriteria dc) {
		return new PropertiesSubqueryExpression(propertyNames, "<>", dc);
	}

	public static Criterion propertiesIn(String[] propertyNames, DetachedCriteria dc) {
		return new PropertiesSubqueryExpression(propertyNames, "in", dc);
	}

	public static Criterion propertiesNotIn(String[] propertyNames, DetachedCriteria dc) {
		return new PropertiesSubqueryExpression(propertyNames, "not in", dc);
	}
	
	public static Criterion propertyNe(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<>", null, dc);
	}
	
	public static Criterion propertyGt(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">", null, dc);
	}
	
	public static Criterion propertyLt(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<", null, dc);
	}
	
	public static Criterion propertyGe(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">=", null, dc);
	}
	
	public static Criterion propertyLe(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<=", null, dc);
	}
	
	public static Criterion propertyGtAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">", "all", dc);
	}
	
	public static Criterion propertyLtAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<", "all", dc);
	}
	
	public static Criterion propertyGeAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">=", "all", dc);
	}
	
	public static Criterion propertyLeAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<=", "all", dc);
	}
	
	public static Criterion propertyGtSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">", "some", dc);
	}
	
	public static Criterion propertyLtSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<", "some", dc);
	}
	
	public static Criterion propertyGeSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">=", "some", dc);
	}
	
	public static Criterion propertyLeSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<=", "some", dc);
	}
	
	public static Criterion eqAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "=", "all", dc);
	}
	
	public static Criterion in(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "in", null, dc);
	}
	
	public static Criterion notIn(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "not in", null, dc);
	}
	
	public static Criterion eq(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "=", null, dc);
	}
	
	public static Criterion gt(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">", null, dc);
	}
	
	public static Criterion lt(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<", null, dc);
	}
	
	public static Criterion ge(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">=", null, dc);
	}
	
	public static Criterion le(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<=", null, dc);
	}
	
	public static Criterion ne(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<>", null, dc);
	}
	
	public static Criterion gtAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">", "all", dc);
	}
	
	public static Criterion ltAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<", "all", dc);
	}
	
	public static Criterion geAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">=", "all", dc);
	}
	
	public static Criterion leAll(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<=", "all", dc);
	}
	
	public static Criterion gtSome(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">", "some", dc);
	}
	
	public static Criterion ltSome(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<", "some", dc);
	}
	
	public static Criterion geSome(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">=", "some", dc);
	}
	
	public static Criterion leSome(Object value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<=", "some", dc);
	}
	

}
