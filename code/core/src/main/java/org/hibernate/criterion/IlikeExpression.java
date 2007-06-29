//$Id: IlikeExpression.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.criterion;


import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.TypedValue;

/**
 * A case-insensitive "like"
 * @author Gavin King
 */
public class IlikeExpression implements Criterion {

	private final String propertyName;
	private final Object value;

	protected IlikeExpression(String propertyName, Object value) {
		this.propertyName = propertyName;
		this.value = value;
	}

	protected IlikeExpression(String propertyName, String value, MatchMode matchMode) {
		this( propertyName, matchMode.toMatchString(value) );
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		Dialect dialect = criteriaQuery.getFactory().getDialect();
		String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		if (columns.length!=1) throw new HibernateException("ilike may only be used with single-column properties");
		if ( dialect instanceof PostgreSQLDialect ) {
			return columns[0] + " ilike ?";
		}
		else {
			return dialect.getLowercaseFunction() + '(' + columns[0] + ") like ?";
		}

		//TODO: get SQL rendering out of this package!
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		return new TypedValue[] { criteriaQuery.getTypedValue( criteria, propertyName, value.toString().toLowerCase() ) };
	}

	public String toString() {
		return propertyName + " ilike " + value;
	}

}
