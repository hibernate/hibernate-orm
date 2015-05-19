/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.util.ArrayList;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * Constrains the property to a specified list of values
 *
 * @author Gavin King
 */
public class InExpression implements Criterion {
	private final String propertyName;
	private final Object[] values;

	/**
	 * Constructs an InExpression
	 *
	 * @param propertyName The property name to check
	 * @param values The values to check against
	 *
	 * @see Restrictions#in(String, java.util.Collection)
	 * @see Restrictions#in(String, Object[])
	 */
	protected InExpression(String propertyName, Object[] values) {
		this.propertyName = propertyName;
		this.values = values;
	}

	@Override
	public String toSqlString( Criteria criteria, CriteriaQuery criteriaQuery ) {
		final String[] columns = criteriaQuery.findColumns( propertyName, criteria );
		if ( criteriaQuery.getFactory().getDialect().supportsRowValueConstructorSyntaxInInList() || columns.length <= 1 ) {
			String singleValueParam = StringHelper.repeat( "?, ", columns.length - 1 ) + "?";
			if ( columns.length > 1 ) {
				singleValueParam = '(' + singleValueParam + ')';
			}
			final String params = values.length > 0
					? StringHelper.repeat( singleValueParam + ", ", values.length - 1 ) + singleValueParam
					: "";
			String cols = StringHelper.join( ", ", columns );
			if ( columns.length > 1 ) {
				cols = '(' + cols + ')';
			}
			return cols + " in (" + params + ')';
		}
		else {
			String cols = " ( " + StringHelper.join( " = ? and ", columns ) + "= ? ) ";
			cols = values.length > 0
					? StringHelper.repeat( cols + "or ", values.length - 1 ) + cols
					: "";
			cols = " ( " + cols + " ) ";
			return cols;
		}
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		final ArrayList<TypedValue> list = new ArrayList<TypedValue>();
		final Type type = criteriaQuery.getTypeUsingProjection( criteria, propertyName );
		if ( type.isComponentType() ) {
			final CompositeType compositeType = (CompositeType) type;
			final Type[] subTypes = compositeType.getSubtypes();
			for ( Object value : values ) {
				for ( int i = 0; i < subTypes.length; i++ ) {
					final Object subValue = value == null
							? null
							: compositeType.getPropertyValues( value, EntityMode.POJO )[i];
					list.add( new TypedValue( subTypes[i], subValue ) );
				}
			}
		}
		else {
			for ( Object value : values ) {
				list.add( new TypedValue( type, value ) );
			}
		}

		return list.toArray( new TypedValue[ list.size() ] );
	}

	@Override
	public String toString() {
		return propertyName + " in (" + StringHelper.toString( values ) + ')';
	}

}
