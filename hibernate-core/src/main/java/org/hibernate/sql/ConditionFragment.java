/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * @author Gavin King
 */
public class ConditionFragment {
	private String tableAlias;
	private String[] lhs;
	private String[] rhs;
	private String op = "=";

	/**
	 * Sets the op.
	 * @param op The op to set
	 */
	public ConditionFragment setOp(String op) {
		this.op = op;
		return this;
	}

	/**
	 * Sets the tableAlias.
	 * @param tableAlias The tableAlias to set
	 */
	public ConditionFragment setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
		return this;
	}

	public ConditionFragment setCondition(String[] lhs, String[] rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
		return this;
	}

	public ConditionFragment setCondition(String[] lhs, String rhs) {
		this.lhs = lhs;
		this.rhs = ArrayHelper.fillArray(rhs, lhs.length);
		return this;
	}

	public String toFragmentString() {
		StringBuilder buf = new StringBuilder( lhs.length * 10 );
		for ( int i=0; i<lhs.length; i++ ) {
			buf.append(tableAlias)
				.append('.')
				.append( lhs[i] )
				.append(op)
				.append( rhs[i] );
			if (i<lhs.length-1) {
				buf.append(" and ");
			}
		}
		return buf.toString();
	}

}
