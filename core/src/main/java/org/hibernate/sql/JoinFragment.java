//$Id: JoinFragment.java 6034 2005-03-07 12:31:37Z pgmjsd $
package org.hibernate.sql;

import org.hibernate.util.StringHelper;

/**
 * An abstract SQL join fragment renderer
 *
 * @author Gavin King
 */
public abstract class JoinFragment {

	public abstract void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, int joinType);

	public abstract void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, int joinType, String on);

	public abstract void addCrossJoin(String tableName, String alias);

	public abstract void addJoins(String fromFragment, String whereFragment);

	public abstract String toFromFragmentString();

	public abstract String toWhereFragmentString();

	// --Commented out by Inspection (12/4/04 9:10 AM): public abstract void addCondition(String alias, String[] columns, String condition);
	public abstract void addCondition(String alias, String[] fkColumns, String[] pkColumns);

	public abstract boolean addCondition(String condition);
	// --Commented out by Inspection (12/4/04 9:10 AM): public abstract void addFromFragmentString(String fromFragmentString);

	public abstract JoinFragment copy();

	public static final int INNER_JOIN = 0;
	public static final int FULL_JOIN = 4;
	public static final int LEFT_OUTER_JOIN = 1;
	public static final int RIGHT_OUTER_JOIN = 2;

	private boolean hasFilterCondition = false;
	private boolean hasThetaJoins = false;

	public void addFragment(JoinFragment ojf) {
		if ( ojf.hasThetaJoins() ) {
			hasThetaJoins = true;
		}
		addJoins( ojf.toFromFragmentString(), ojf.toWhereFragmentString() );
	}

	/**
	 * Appends the 'on' condition to the buffer, returning true if the condition was added.
	 * Returns false if the 'on' condition was empty.
	 *
	 * @param buffer The buffer to append the 'on' condition to.
	 * @param on     The 'on' condition.
	 * @return Returns true if the condition was added, false if the condition was already in 'on' string.
	 */
	protected boolean addCondition(StringBuffer buffer, String on) {
		if ( StringHelper.isNotEmpty( on ) ) {
			if ( !on.startsWith( " and" ) ) buffer.append( " and " );
			buffer.append( on );
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * True if the where fragment is from a filter condition.
	 *
	 * @return True if the where fragment is from a filter condition.
	 */
	public boolean hasFilterCondition() {
		return hasFilterCondition;
	}

	public void setHasFilterCondition(boolean b) {
		this.hasFilterCondition = b;
	}

	public boolean hasThetaJoins() {
		return hasThetaJoins;
	}

	public void setHasThetaJoins(boolean hasThetaJoins) {
		this.hasThetaJoins = hasThetaJoins;
	}
}
