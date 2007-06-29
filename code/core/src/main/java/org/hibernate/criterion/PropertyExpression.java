//$Id: PropertyExpression.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.criterion;


import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.TypedValue;
import org.hibernate.util.StringHelper;

/**
 * superclass for comparisons between two properties (with SQL binary operators)
 * @author Gavin King
 */
public class PropertyExpression implements Criterion {

	private final String propertyName;
	private final String otherPropertyName;
	private final String op;

	private static final TypedValue[] NO_TYPED_VALUES = new TypedValue[0];

	protected PropertyExpression(String propertyName, String otherPropertyName, String op) {
		this.propertyName = propertyName;
		this.otherPropertyName = otherPropertyName;
		this.op = op;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		String[] xcols = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		String[] ycols = criteriaQuery.getColumnsUsingProjection(criteria, otherPropertyName);
		String result = StringHelper.join(
			" and ",
			StringHelper.add(xcols, getOp(), ycols)
		);
		if (xcols.length>1) result = '(' + result + ')';
		return result;
		//TODO: get SQL rendering out of this package!
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return NO_TYPED_VALUES;
	}

	public String toString() {
		return propertyName + getOp() + otherPropertyName;
	}

	public String getOp() {
		return op;
	}

}
