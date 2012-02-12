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
import org.hibernate.AssertionFailure;

/**
 * An ANSI-style join
 *
 * @author Gavin King
 */
public class ANSIJoinFragment extends JoinFragment {

	private StringBuilder buffer = new StringBuilder();
	private StringBuilder conditions = new StringBuilder();

	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType) {
		addJoin(tableName, alias, fkColumns, pkColumns, joinType, null);
	}

	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType, String on) {
		String joinString;
		switch (joinType) {
			case INNER_JOIN:
				joinString = " inner join ";
				break;
			case LEFT_OUTER_JOIN:
				joinString = " left outer join ";
				break;
			case RIGHT_OUTER_JOIN:
				joinString = " right outer join ";
				break;
			case FULL_JOIN:
				joinString = " full outer join ";
				break;
			default:
				throw new AssertionFailure("undefined join type");
		}

		buffer.append(joinString)
			.append(tableName)
			.append(' ')
			.append(alias)
			.append(" on ");


		for ( int j=0; j<fkColumns.length; j++) {
			/*if ( fkColumns[j].indexOf('.')<1 ) {
				throw new AssertionFailure("missing alias");
			}*/
			buffer.append( fkColumns[j] )
				.append('=')
				.append(alias)
				.append('.')
				.append( pkColumns[j] );
			if ( j<fkColumns.length-1 ) buffer.append(" and ");
		}

		addCondition(buffer, on);

	}

	public String toFromFragmentString() {
		return buffer.toString();
	}

	public String toWhereFragmentString() {
		return conditions.toString();
	}

	public void addJoins(String fromFragment, String whereFragment) {
		buffer.append(fromFragment);
		//where fragment must be empty!
	}

	public JoinFragment copy() {
		ANSIJoinFragment copy = new ANSIJoinFragment();
		copy.buffer = new StringBuilder( buffer.toString() );
		return copy;
	}

	public void addCondition(String alias, String[] columns, String condition) {
		for ( int i=0; i<columns.length; i++ ) {
			conditions.append(" and ")
				.append(alias)
				.append('.')
				.append( columns[i] )
				.append(condition);
		}
	}

	public void addCrossJoin(String tableName, String alias) {
		buffer.append(", ")
			.append(tableName)
			.append(' ')
			.append(alias);
	}

	public void addCondition(String alias, String[] fkColumns, String[] pkColumns) {
		throw new UnsupportedOperationException();

	}

	public boolean addCondition(String condition) {
		return addCondition(conditions, condition);
	}

	public void addFromFragmentString(String fromFragmentString) {
		buffer.append(fromFragmentString);
	}

}






