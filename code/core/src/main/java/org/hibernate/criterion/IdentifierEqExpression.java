//$Id: IdentifierEqExpression.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.criterion;


import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.TypedValue;
import org.hibernate.util.StringHelper;

/**
 * An identifier constraint
 * @author Gavin King
 */
public class IdentifierEqExpression implements Criterion {

	private final Object value;

	protected IdentifierEqExpression(Object value) {
		this.value = value;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {

		String[] columns = criteriaQuery.getIdentifierColumns(criteria);

		String result = StringHelper.join(
			" and ",
			StringHelper.suffix( columns, " = ?" )
		);
		if (columns.length>1) result = '(' + result + ')';
		return result;

		//TODO: get SQL rendering out of this package!
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		return new TypedValue[] { criteriaQuery.getTypedIdentifierValue(criteria, value) };
	}

	public String toString() {
		return "id = " + value;
	}

}
