//$Id: NotExpression.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.criterion;


import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.TypedValue;

/**
 * Negates another criterion
 * @author Gavin King
 */
public class NotExpression implements Criterion {

	private Criterion criterion;

	protected NotExpression(Criterion criterion) {
		this.criterion = criterion;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		if ( criteriaQuery.getFactory().getDialect() instanceof MySQLDialect ) {
			return "not (" + criterion.toSqlString(criteria, criteriaQuery) + ')';
		}
		else {
			return "not " + criterion.toSqlString(criteria, criteriaQuery);
		}
	}

	public TypedValue[] getTypedValues(
		Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		return criterion.getTypedValues(criteria, criteriaQuery);
	}

	public String toString() {
		return "not " + criterion.toString();
	}

}
