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
