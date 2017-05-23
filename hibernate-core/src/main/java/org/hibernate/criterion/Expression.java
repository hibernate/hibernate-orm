/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
