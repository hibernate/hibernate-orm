//$Id: Sybase11JoinFragment.java 4886 2004-12-05 15:04:21Z pgmjsd $
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


