/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.util.Locale;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
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
		if ( dialect instanceof PostgreSQLDialect || dialect instanceof PostgreSQL81Dialect) {
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
						value.toString().toLowerCase(Locale.ROOT)
				)
		};
	}

	@Override
	public String toString() {
		return propertyName + " ilike " + value;
	}

}
