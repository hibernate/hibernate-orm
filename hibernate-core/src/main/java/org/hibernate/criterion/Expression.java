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

import org.hibernate.type.Type;

/**
 * Factory for Criterion objects.  Deprecated!
 *
 * @author Gavin King
 *
 * @see Restrictions
 *
 * @deprecated Use {@link Restrictions} instead
 */
@Deprecated
public final class Expression extends Restrictions {
	/**
	 * Apply a constraint expressed in SQL, with JDBC parameters.  Any occurrences of <tt>{alias}</tt> will be
	 * replaced by the table alias.
	 *
	 * @param sql The sql
	 * @param values The parameter values
	 * @param types The parameter types
	 *
	 * @return Criterion
	 *
	 * @deprecated use {@link org.hibernate.criterion.Restrictions#sqlRestriction(String, Object[], Type[])}
	 */
	@Deprecated
	public static Criterion sql(String sql, Object[] values, Type[] types) {
		return new SQLCriterion( sql, values, types );
	}

	/**
	 * Apply a constraint expressed in SQL, with a JDBC parameter.  Any occurrences of <tt>{alias}</tt> will be
	 * replaced by the table alias.
	 *
	 * @param sql The sql
	 * @param value The parameter value
	 * @param type The parameter type
	 *
	 * @return Criterion
	 *
	 * @deprecated use {@link org.hibernate.criterion.Restrictions#sqlRestriction(String, Object, Type)}
	 */
	@Deprecated
	public static Criterion sql(String sql, Object value, Type type) {
		return new SQLCriterion( sql, value, type );
	}

	/**
	 * Apply a constraint expressed in SQL with no parameters.  Any occurrences of <tt>{alias}</tt> will be
	 * replaced by the table alias.
	 *
	 * @param sql The sql
	 *
	 * @return Criterion
	 *
	 * @deprecated use {@link org.hibernate.criterion.Restrictions#sqlRestriction(String)}
	 */
	@Deprecated
	public static Criterion sql(String sql) {
		return new SQLCriterion( sql );
	}

	private Expression() {
		//cannot be instantiated
	}
}
