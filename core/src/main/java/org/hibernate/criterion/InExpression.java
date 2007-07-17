//$Id: InExpression.java 7557 2005-07-19 23:25:36Z oneovthafew $
package org.hibernate.criterion;

import java.util.ArrayList;



import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;


import org.hibernate.engine.TypedValue;

import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * Constrains the property to a specified list of values
 * @author Gavin King
 */
public class InExpression implements Criterion {

	private final String propertyName;
	private final Object[] values;

	protected InExpression(String propertyName, Object[] values) {
		this.propertyName = propertyName;
		this.values = values;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		String singleValueParam = StringHelper.repeat( "?, ", columns.length-1 )  + "?";
		if ( columns.length>1 ) singleValueParam = '(' + singleValueParam + ')';
		String params = values.length>0 ?
			StringHelper.repeat( singleValueParam + ", ", values.length-1 ) + singleValueParam :
			"";
		String cols = StringHelper.join(", ", columns);
		if ( columns.length>1 ) cols = '(' + cols + ')';
		return cols + " in (" + params + ')';
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		ArrayList list = new ArrayList();
		Type type = criteriaQuery.getTypeUsingProjection(criteria, propertyName);
		if ( type.isComponentType() ) {
			AbstractComponentType actype = (AbstractComponentType) type;
			Type[] types = actype.getSubtypes();
			for ( int i=0; i<types.length; i++ ) {
				for ( int j=0; j<values.length; j++ ) {
					Object subval = values[j]==null ? 
						null : 
						actype.getPropertyValues( values[j], EntityMode.POJO )[i];
					list.add( new TypedValue( types[i], subval, EntityMode.POJO ) );
				}
			}
		}
		else {
			for ( int j=0; j<values.length; j++ ) {
				list.add( new TypedValue( type, values[j], EntityMode.POJO ) );
			}
		}
		return (TypedValue[]) list.toArray( new TypedValue[ list.size() ] );
	}

	public String toString() {
		return propertyName + " in (" + StringHelper.toString(values) + ')';
	}

}
