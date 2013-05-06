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
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.engine.spi.TypedValue;

/**
 * A case-insensitive "like".
 *
 * @author Gavin King
 *
 * @deprecated Prefer {@link LikeExpression} which now has case-insensitivity capability.
 */
@Deprecated
@SuppressWarnings({"deprecation", "UnusedDeclaration"})
public class IlikeExpression implements Criterion {
	private final String propertyName;
	private final Object value;

	protected IlikeExpression(String propertyName, Object value) {
		this.propertyName = propertyName;
		this.value = value;
	}

	protected IlikeExpression(String propertyName, String value, MatchMode matchMode) {
		this( propertyName, matchMode.toMatchString( value ) );
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		final Dialect dialect = criteriaQuery.getFactory().getDialect();
		final String[] columns = criteriaQuery.findColumns( propertyName, criteria );
		if ( columns.length != 1 ) {
			throw new HibernateException( "ilike may only be used with single-column properties" );
		}
		if ( dialect instanceof PostgreSQL81Dialect) {
			return columns[0] + " ilike ?";
		}
		else {
			return dialect.getLowercaseFunction() + '(' + columns[0] + ") like ?";
		}
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		return new TypedValue[] {
				criteriaQuery.getTypedValue(
						criteria,
						propertyName,
						value.toString().toLowerCase()
				)
		};
	}

	@Override
	public String toString() {
		return propertyName + " ilike " + value;
	}

}
