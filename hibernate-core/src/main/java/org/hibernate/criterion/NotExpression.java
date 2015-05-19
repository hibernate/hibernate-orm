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

/**
 * A criterion that is a wrapper for another, negating the wrapped one.
 *
 * @author Gavin King
 */
public class NotExpression implements Criterion {
	private Criterion criterion;

	/**
	 * Constructs a NotExpression
	 *
	 * @param criterion The expression to wrap and negate
	 *
	 * @see Restrictions#not
	 */
	protected NotExpression(Criterion criterion) {
		this.criterion = criterion;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return criteriaQuery.getFactory().getDialect().getNotExpression(
				criterion.toSqlString( criteria, criteriaQuery )
		);
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return criterion.getTypedValues( criteria, criteriaQuery );
	}

	@Override
	public String toString() {
		return "not " + criterion.toString();
	}

}
