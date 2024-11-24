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

public class ForeignKeyNullExpression implements Criterion {
	private static final TypedValue[] NO_VALUES = new TypedValue[0];

	private final String associationPropertyName;
	private final boolean negated;

	public ForeignKeyNullExpression(String associationPropertyName) {
		this.associationPropertyName = associationPropertyName;
		this.negated = false;
	}

	public ForeignKeyNullExpression(String associationPropertyName, boolean negated) {
		this.associationPropertyName = associationPropertyName;
		this.negated = negated;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		final String[] columns = criteriaQuery.getForeignKeyColumns( criteria, associationPropertyName );

		String result = String.join( " and ", StringHelper.suffix( columns, getSuffix() ) );
		if ( columns.length > 1 ) {
			result = '(' + result + ')';
		}
		return result;
	}

	private String getSuffix() {
		if ( negated ) {
			return " is not null";
		}
		return " is null";
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		return NO_VALUES;
	}

}
