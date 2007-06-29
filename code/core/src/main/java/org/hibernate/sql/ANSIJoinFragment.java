//$Id: ANSIJoinFragment.java 4886 2004-12-05 15:04:21Z pgmjsd $
package org.hibernate.sql;

import org.hibernate.AssertionFailure;

/**
 * An ANSI-style join
 *
 * @author Gavin King
 */
public class ANSIJoinFragment extends JoinFragment {

	private StringBuffer buffer = new StringBuffer();
	private StringBuffer conditions = new StringBuffer();

	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, int joinType) {
		addJoin(tableName, alias, fkColumns, pkColumns, joinType, null);
	}

	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, int joinType, String on) {
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
		copy.buffer = new StringBuffer( buffer.toString() );
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






