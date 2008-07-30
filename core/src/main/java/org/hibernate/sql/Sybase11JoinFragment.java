/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.sql;


/**
 * An old Sybase-style join (before Sybase supported the ANSI style "inner join" etc syntax)
 * This is needed for Sybase 11.9.2 and earlier, using the HQL 2.* syntax with Collections.
 *
 * @author Colm O' Flaherty
 */
public class Sybase11JoinFragment extends JoinFragment {

	private StringBuffer afterFrom = new StringBuffer();
	private StringBuffer afterWhere = new StringBuffer();

	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, int joinType) {

		addCrossJoin(tableName, alias);

		for ( int j=0; j<fkColumns.length; j++) {
			//full joins are not supported.. yet!
			if (joinType==JoinFragment.FULL_JOIN ) throw new UnsupportedOperationException();

			afterWhere.append(" and ")
				.append( fkColumns[j] )
				.append( " " );

			if (joinType==LEFT_OUTER_JOIN ) afterWhere.append("*");
			afterWhere.append('=');
			if (joinType==RIGHT_OUTER_JOIN ) afterWhere.append("*");

			afterWhere.append (" ")
				.append(alias)
				.append('.')
				.append( pkColumns[j] );
		}

	}

	public String toFromFragmentString() {
		return afterFrom.toString();
	}

	public String toWhereFragmentString() {
		return afterWhere.toString();
	}

	public void addJoins(String fromFragment, String whereFragment) {
		afterFrom.append(fromFragment);
		afterWhere.append(whereFragment);
	}

	public JoinFragment copy() {
		Sybase11JoinFragment copy = new Sybase11JoinFragment();
		copy.afterFrom = new StringBuffer( afterFrom.toString() );
		copy.afterWhere = new StringBuffer( afterWhere.toString() );
		return copy;
	}

	public void addCondition(String alias, String[] columns, String condition) {
		for ( int i=0; i<columns.length; i++ ) {
			afterWhere.append(" and ")
				.append(alias)
				.append('.')
				.append( columns[i] )
				.append(condition);
		}
	}

	public void addCrossJoin(String tableName, String alias) {
		afterFrom.append(", ")
			.append(tableName)
			.append(' ')
			.append(alias);
	}

	public void addCondition(String alias, String[] fkColumns, String[] pkColumns) {
		throw new UnsupportedOperationException();

	}

	public boolean addCondition(String condition) {
		return addCondition(afterWhere, condition);
	}


	public void addFromFragmentString(String fromFragmentString) {
		afterFrom.append(fromFragmentString);
	}


	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, int joinType, String on) {
		addJoin(tableName, alias, fkColumns, pkColumns, joinType);
		addCondition(on);
	}
}


