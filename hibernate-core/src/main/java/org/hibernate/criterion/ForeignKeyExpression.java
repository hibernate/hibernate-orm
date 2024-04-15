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

public class ForeignKeyExpression implements Criterion {
	private final String associationPropertyName;
	private final Object value;
	private final String operator;

	public ForeignKeyExpression(String associationPropertyName, Object value, String operator) {
		this.associationPropertyName = associationPropertyName;
		this.value = value;
		this.operator = operator;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		final String[] columns = criteriaQuery.getForeignKeyColumns( criteria, associationPropertyName );

		String result = String.join( " and ", StringHelper.suffix( columns, operator + "  ?" ) );
		if ( columns.length > 1 ) {
			result = '(' + result + ')';
		}
		return result;
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		return new TypedValue[] { criteriaQuery.getForeignKeyTypeValue( criteria, associationPropertyName, value ) };
	}

}
