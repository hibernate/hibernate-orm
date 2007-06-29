//$Id: Subqueries.java 8467 2005-10-26 21:20:17Z oneovthafew $
package org.hibernate.criterion;

/**
 * Factory class for criterion instances that represent expressions
 * involving subqueries.
 * 
 * @see Restriction
 * @see Projection
 * @see org.hibernate.Criteria
 * @author Gavin King
 */
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
