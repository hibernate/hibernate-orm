/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.Type;

/**
 * A SQL fragment. The string {alias} will be replaced by the
 * alias of the root entity.
 */
public class SQLCriterion implements Criterion {
	private final String sql;
	private final TypedValue[] typedValues;

	protected SQLCriterion(String sql, Object[] values, Type[] types) {
		this.sql = sql;
		this.typedValues = new TypedValue[values.length];
		for ( int i=0; i<typedValues.length; i++ ) {
			typedValues[i] = new TypedValue( types[i], values[i] );
		}
	}

	protected SQLCriterion(String sql, Object value, Type type) {
		this.sql = sql;
		this.typedValues = new TypedValue[] { new TypedValue( type, value ) };
	}

	protected SQLCriterion(String sql) {
		this.sql = sql;
		this.typedValues = new TypedValue[0];
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		return StringHelper.replace( sql, "{alias}", criteriaQuery.getSQLAlias( criteria ) );
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		return typedValues;
	}

	@Override
	public String toString() {
		return sql;
	}
}
