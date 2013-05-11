/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.Type;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A SQL fragment. For any criteria aliases that exist, the SQL string will look for the pattern
 * {myAlias} and replace it with the corresponding table alias that was generated for the query.
 * As a special case, the string '{alias}' will be replaced by the SQL alias of the root entity.
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
		String updatedSQL = StringHelper.replace( sql, "{alias}", criteriaQuery.getSQLAlias( criteria ) );

		Iterator<String> aliasIterator = StringHelper.findInterpolationKeys( updatedSQL ).iterator();
		while( aliasIterator.hasNext() ) {
			String alias = aliasIterator.next();
			String sqlAlias = criteriaQuery.getSQLAlias( alias );
			if( sqlAlias != null ) {
				updatedSQL = StringHelper.interpolateAlias( updatedSQL, alias, sqlAlias );
			}
		}

		return updatedSQL;
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
