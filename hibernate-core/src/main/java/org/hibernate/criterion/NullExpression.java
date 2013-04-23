/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.criterion;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;

/**
 * Constrains a property to be null
 *
 * @author Gavin King
 */
public class NullExpression implements Criterion {
	private static final TypedValue[] NO_VALUES = new TypedValue[0];

	private final String propertyName;

	/**
	 * Constructs a NullExpression
	 *
	 * @param propertyName The name of the property to check for null
	 *
	 * @see Restrictions#isNull
	 */
	protected NullExpression(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String[] columns = criteriaQuery.findColumns( propertyName, criteria );
		String result = StringHelper.join(
				" and ",
				StringHelper.suffix( columns, " is null" )
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
		return propertyName + " is null";
	}

}
