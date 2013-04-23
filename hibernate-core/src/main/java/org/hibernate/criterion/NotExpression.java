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
