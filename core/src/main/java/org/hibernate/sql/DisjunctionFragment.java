//$Id: DisjunctionFragment.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.sql;

/**
 * A disjunctive string of conditions
 * @author Gavin King
 */
public class DisjunctionFragment {

	private StringBuffer buffer = new StringBuffer();

	public DisjunctionFragment addCondition(ConditionFragment fragment) {
		if ( buffer.length()>0 ) buffer.append(" or ");
		buffer.append("(")
			.append( fragment.toFragmentString() )
			.append(")");
		return this;
	}

	public String toFragmentString() {
		return buffer.toString();
	}
}
