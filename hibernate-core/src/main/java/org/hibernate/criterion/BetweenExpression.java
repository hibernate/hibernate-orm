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
 * Constrains a property to between two values
 *
 * @author Gavin King
 */
public class BetweenExpression implements Criterion {
	private final String propertyName;
	private final Object lo;
	private final Object hi;

	protected BetweenExpression(String propertyName, Object lo, Object hi) {
		this.propertyName = propertyName;
		this.lo = lo;
		this.hi = hi;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String[] columns = criteriaQuery.findColumns( propertyName, criteria );
		final String[] expressions = StringHelper.suffix( columns, " between ? and ?" );
		return StringHelper.join( " and ", expressions );
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] {
				criteriaQuery.getTypedValue( criteria, propertyName, lo ),
				criteriaQuery.getTypedValue( criteria, propertyName, hi )
		};
	}

	@Override
	public String toString() {
		return propertyName + " between " + lo + " and " + hi;
	}

}
