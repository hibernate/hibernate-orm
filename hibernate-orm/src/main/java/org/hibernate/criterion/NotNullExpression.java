/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;

/**
 * Constrains a property to be non-null
 *
 * @author Gavin King
 */
public class NotNullExpression implements Criterion {
	private static final TypedValue[] NO_VALUES = new TypedValue[0];

	private final String propertyName;

	protected NotNullExpression(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String[] columns = criteriaQuery.findColumns( propertyName, criteria );
		String result = StringHelper.join(
				" or ",
				StringHelper.suffix( columns, " is not null" )
		);
		if ( columns.length > 1 ) {
			result = '(' + result + ')';
		}
		return result;
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return NO_VALUES;
	}

	@Override
	public String toString() {
		return propertyName + " is not null";
	}

}
