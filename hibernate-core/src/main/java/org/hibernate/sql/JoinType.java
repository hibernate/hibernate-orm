/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import org.hibernate.HibernateException;

/**
 * @author Strong Liu
 */

public enum JoinType {
	NONE( -666, null ),
	INNER_JOIN( 0, "inner" ),
	LEFT_OUTER_JOIN( 1, "left" ),
	RIGHT_OUTER_JOIN( 2, "right" ),
	FULL_JOIN( 4, "full" );

	private final int joinTypeValue;
	private final String sqlText;

	JoinType(int joinTypeValue, String sqlText) {
		this.joinTypeValue = joinTypeValue;
		this.sqlText = sqlText;
	}

	public int getJoinTypeValue() {
		return joinTypeValue;
	}

	public String getSqlText() {
		return sqlText;
	}

	public static JoinType parse(int joinType) {
		if ( joinType < 0 ) {
			return NONE;
		}
		switch ( joinType ) {
			case 0:
				return INNER_JOIN;
			case 1:
				return LEFT_OUTER_JOIN;
			case 2:
				return RIGHT_OUTER_JOIN;
			case 4:
				return FULL_JOIN;
			default:
				throw new HibernateException( "unknown join type: " + joinType );
		}
	}
}
