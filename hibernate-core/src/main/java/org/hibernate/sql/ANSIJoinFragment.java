/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import org.hibernate.AssertionFailure;

/**
 * An ANSI-style join.
 *
 * @author Gavin King
 */
public class ANSIJoinFragment extends JoinFragment {
	private StringBuilder buffer = new StringBuilder();
	private StringBuilder conditions = new StringBuilder();

	/**
	 * Adds a join, represented by the given information, to the fragment.
	 *
	 * @param tableName The name of the table being joined.
	 * @param alias The alias applied to the table being joined.
	 * @param fkColumns The columns (from the table being joined) used to define the join-restriction (the ON)
	 * @param pkColumns The columns (from the table being joined to) used to define the join-restriction (the ON)
	 * @param joinType The type of join to produce (INNER, etc).
	 */
	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType) {
		addJoin( tableName, alias, fkColumns, pkColumns, joinType, null );
	}

	/**
	 * Adds a join, represented by the given information, to the fragment.
	 *
	 * @param rhsTableName The name of the table being joined (the RHS table).
	 * @param rhsAlias The alias applied to the table being joined (the alias for the RHS table).
	 * @param lhsColumns The columns (from the table being joined) used to define the join-restriction (the ON).  These
	 * are the LHS columns, and are expected to be qualified.
	 * @param rhsColumns The columns (from the table being joined to) used to define the join-restriction (the ON).  These
	 * are the RHS columns and are expected to *not* be qualified.
	 * @param joinType The type of join to produce (INNER, etc).
	 * @param on Any extra join restrictions
	 */
	public void addJoin(
			String rhsTableName,
			String rhsAlias,
			String[] lhsColumns,
			String[] rhsColumns,
			JoinType joinType,
			String on) {
		final String joinString;
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

		this.buffer.append(joinString)
			.append(rhsTableName)
			.append(' ')
			.append(rhsAlias)
			.append(" on ");


		for ( int j=0; j<lhsColumns.length; j++) {
			this.buffer.append( lhsColumns[j] )
				.append('=')
				.append(rhsAlias)
				.append('.')
				.append( rhsColumns[j] );
			if ( j < lhsColumns.length-1 ) {
				this.buffer.append( " and " );
			}
		}

		addCondition( buffer, on );

	}

	public void addJoin(
			String rhsTableName,
			String rhsAlias,
			String[][] lhsColumns,
			String[] rhsColumns,
			JoinType joinType,
			String on) {
		final String joinString;
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

		this.buffer.append(joinString)
				.append(rhsTableName)
				.append(' ')
				.append(rhsAlias)
				.append(" on ");


		if ( lhsColumns.length > 1 ) {
			this.buffer.append( "(" );
		}
		for ( int i = 0; i < lhsColumns.length; i++ ) {
			for ( int j=0; j<lhsColumns[i].length; j++) {
				this.buffer.append( lhsColumns[i][j] )
						.append('=')
						.append(rhsAlias)
						.append('.')
						.append( rhsColumns[j] );
				if ( j < lhsColumns[i].length-1 ) {
					this.buffer.append( " and " );
				}
			}
			if ( i < lhsColumns.length - 1 ) {
				this.buffer.append( " or " );
			}
		}
		if ( lhsColumns.length > 1 ) {
			this.buffer.append( ")" );
		}

		addCondition( buffer, on );
	}

	@Override
	public String toFromFragmentString() {
		return this.buffer.toString();
	}

	@Override
	public String toWhereFragmentString() {
		return this.conditions.toString();
	}

	@Override
	public void addJoins(String fromFragment, String whereFragment) {
		this.buffer.append( fromFragment );
		//where fragment must be empty!
	}

	@Override
	public JoinFragment copy() {
		final ANSIJoinFragment copy = new ANSIJoinFragment();
		copy.buffer = new StringBuilder( this.buffer.toString() );
		return copy;
	}

	/**
	 * Adds a condition to the join fragment.  For each given column a predicate is built in the form:
	 * {@code [alias.[column] = [condition]}
	 *
	 * @param alias The alias to apply to column(s)
	 * @param columns The columns to apply restriction
	 * @param condition The restriction condition
	 */
	public void addCondition(String alias, String[] columns, String condition) {
		for ( String column : columns ) {
			this.conditions.append( " and " )
					.append( alias )
					.append( '.' )
					.append( column )
					.append( condition );
		}
	}

	@Override
	public void addCrossJoin(String tableName, String alias) {
		this.buffer.append(", ")
			.append(tableName)
			.append(' ')
			.append(alias);
	}

	@Override
	public void addCondition(String alias, String[] fkColumns, String[] pkColumns) {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean addCondition(String condition) {
		return addCondition(conditions, condition);
	}

	/**
	 * Adds an externally built join fragment.
	 *
	 * @param fromFragmentString The join fragment string
	 */
	public void addFromFragmentString(String fromFragmentString) {
		this.buffer.append(fromFragmentString);
	}

}
