//$Id: OracleJoinFragment.java 6750 2005-05-11 15:26:04Z oneovthafew $
package org.hibernate.sql;

import java.util.HashSet;
import java.util.Set;

/**
 * An Oracle-style (theta) join
 *
 * @author Jon Lipsky, Gavin King
 */
public class OracleJoinFragment extends JoinFragment {

	private StringBuffer afterFrom = new StringBuffer();
	private StringBuffer afterWhere = new StringBuffer();

	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, int joinType) {

		addCrossJoin( tableName, alias );

		for ( int j = 0; j < fkColumns.length; j++ ) {
			setHasThetaJoins( true );
			afterWhere.append( " and " )
					.append( fkColumns[j] );
			if ( joinType == RIGHT_OUTER_JOIN || joinType == FULL_JOIN ) afterWhere.append( "(+)" );
			afterWhere.append( '=' )
					.append( alias )
					.append( '.' )
					.append( pkColumns[j] );
			if ( joinType == LEFT_OUTER_JOIN || joinType == FULL_JOIN ) afterWhere.append( "(+)" );
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
		OracleJoinFragment copy = new OracleJoinFragment();
		copy.afterFrom = new StringBuffer( afterFrom.toString() );
		copy.afterWhere = new StringBuffer( afterWhere.toString() );
		return copy;
	}

	public void addCondition(String alias, String[] columns, String condition) {
		for ( int i = 0; i < columns.length; i++ ) {
			afterWhere.append( " and " )
					.append( alias )
					.append( '.' )
					.append( columns[i] )
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

	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, int joinType, String on) {
		//arbitrary on clause ignored!!
		addJoin( tableName, alias, fkColumns, pkColumns, joinType );
		if ( joinType == JoinFragment.INNER_JOIN ) {
			addCondition( on );
		}
		else if ( joinType == JoinFragment.LEFT_OUTER_JOIN ) {
			addLeftOuterJoinCondition( on );
		}
		else {
			throw new UnsupportedOperationException( "join type not supported by OracleJoinFragment (use Oracle9Dialect)" );
		}
	}

	/**
	 * This method is a bit of a hack, and assumes
	 * that the column on the "right" side of the
	 * join appears on the "left" side of the
	 * operator, which is extremely wierd if this
	 * was a normal join condition, but is natural
	 * for a filter.
	 */
	private void addLeftOuterJoinCondition(String on) {
		StringBuffer buf = new StringBuffer( on );
		for ( int i = 0; i < buf.length(); i++ ) {
			char character = buf.charAt( i );
			boolean isInsertPoint = OPERATORS.contains( new Character( character ) ) ||
					( character == ' ' && buf.length() > i + 3 && "is ".equals( buf.substring( i + 1, i + 4 ) ) );
			if ( isInsertPoint ) {
				buf.insert( i, "(+)" );
				i += 3;
			}
		}
		addCondition( buf.toString() );
	}

	private static final Set OPERATORS = new HashSet();

	static {
		OPERATORS.add( new Character( '=' ) );
		OPERATORS.add( new Character( '<' ) );
		OPERATORS.add( new Character( '>' ) );
	}
}
