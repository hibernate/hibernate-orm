/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;


/**
 * An old Sybase-style join (before Sybase supported the ANSI style "inner join" etc syntax)
 * This is needed for Sybase 11.9.2 and earlier, using the HQL 2.* syntax with Collections.
 *
 * @author Colm O' Flaherty
 */
public class Sybase11JoinFragment extends JoinFragment {

	private StringBuilder afterFrom = new StringBuilder();
	private StringBuilder afterWhere = new StringBuilder();

	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType) {

		addCrossJoin( tableName, alias );

		for ( int j = 0; j < fkColumns.length; j++ ) {
			//full joins are not supported.. yet!
			if ( joinType == JoinType.FULL_JOIN ) {
				throw new UnsupportedOperationException();
			}

			afterWhere.append( " and " )
					.append( fkColumns[j] )
					.append( " " );

			if ( joinType == JoinType.LEFT_OUTER_JOIN ) {
				afterWhere.append( '*' );
			}
			afterWhere.append( '=' );
			if ( joinType == JoinType.RIGHT_OUTER_JOIN ) {
				afterWhere.append( "*" );
			}

			afterWhere.append( " " )
					.append( alias )
					.append( '.' )
					.append( pkColumns[j] );
		}
	}

	public void addJoin(String tableName, String alias, String[][] fkColumns, String[] pkColumns, JoinType joinType) {

		addCrossJoin( tableName, alias );

		if ( fkColumns.length > 1 ) {
			afterWhere.append( "(" );
		}
		for ( int i = 0; i < fkColumns.length; i++ ) {
			afterWhere.append( " and " );
			for ( int j = 0; j < fkColumns[i].length; j++ ) {
				//full joins are not supported.. yet!
				if ( joinType == JoinType.FULL_JOIN ) {
					throw new UnsupportedOperationException();
				}

				afterWhere.append( fkColumns[i][j] )
						.append( " " );

				if ( joinType == JoinType.LEFT_OUTER_JOIN ) {
					afterWhere.append( '*' );
				}
				afterWhere.append( '=' );
				if ( joinType == JoinType.RIGHT_OUTER_JOIN ) {
					afterWhere.append( "*" );
				}

				afterWhere.append( " " )
						.append( alias )
						.append( '.' )
						.append( pkColumns[j] );
				if ( j < fkColumns[i].length - 1 ) {
					afterWhere.append( " and " );
				}
			}
			if ( i < fkColumns.length - 1 ) {
				afterWhere.append( " or " );
			}
		}
		if ( fkColumns.length > 1 ) {
			afterWhere.append( ")" );
		}
	}

	public String toFromFragmentString() {
		return afterFrom.toString();
	}

	public String toWhereFragmentString() {
		return afterWhere.toString();
	}

	public void addJoins(String fromFragment, String whereFragment) {
		afterFrom.append( fromFragment );
		afterWhere.append( whereFragment );
	}

	public JoinFragment copy() {
		Sybase11JoinFragment copy = new Sybase11JoinFragment();
		copy.afterFrom = new StringBuilder( afterFrom.toString() );
		copy.afterWhere = new StringBuilder( afterWhere.toString() );
		return copy;
	}

	public void addCondition(String alias, String[] columns, String condition) {
		for ( String column : columns ) {
			afterWhere.append( " and " )
					.append( alias )
					.append( '.' )
					.append( column )
					.append( condition );
		}
	}

	public void addCrossJoin(String tableName, String alias) {
		afterFrom.append( ", " )
				.append( tableName )
				.append( ' ' )
				.append( alias );
	}

	public void addCondition(String alias, String[] fkColumns, String[] pkColumns) {
		throw new UnsupportedOperationException();

	}

	public boolean addCondition(String condition) {
		return addCondition( afterWhere, condition );
	}


	public void addFromFragmentString(String fromFragmentString) {
		afterFrom.append( fromFragmentString );
	}


	public void addJoin(
			String tableName,
			String alias,
			String[] fkColumns,
			String[] pkColumns,
			JoinType joinType,
			String on) {
		addJoin( tableName, alias, fkColumns, pkColumns, joinType );
		addCondition( on );
	}

	public void addJoin(
			String tableName,
			String alias,
			String[][] fkColumns,
			String[] pkColumns,
			JoinType joinType,
			String on) {
		addJoin( tableName, alias, fkColumns, pkColumns, joinType );
		addCondition( on );
	}
}
