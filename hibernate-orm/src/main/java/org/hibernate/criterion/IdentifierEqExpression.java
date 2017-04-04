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

/**
 * An identifier constraint
 *
 * @author Gavin King
 */
public class IdentifierEqExpression implements Criterion {
	private final Object value;

	/**
	 * Constructs an IdentifierEqExpression
	 *
	 * @param value The identifier value
	 *
	 * @see Restrictions#idEq
	 */
	protected IdentifierEqExpression(Object value) {
		this.value = value;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		final String[] columns = criteriaQuery.getIdentifierColumns( criteria );

		String result = StringHelper.join( " and ", StringHelper.suffix( columns, " = ?" ) );
		if ( columns.length > 1) {
			result = '(' + result + ')';
		}
		return result;
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		return new TypedValue[] { criteriaQuery.getTypedIdentifierValue( criteria, value ) };
	}

	@Override
	public String toString() {
		return "id = " + value;
	}

}
