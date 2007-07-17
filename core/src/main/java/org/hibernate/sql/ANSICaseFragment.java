//$Id: ANSICaseFragment.java 4851 2004-12-02 05:09:49Z oneovthafew $
package org.hibernate.sql;

import java.util.Iterator;
import java.util.Map;

/**
 An ANSI SQL CASE expression.
 <br>
 <code>case when ... then ... end as ...</code>
 <br>
 @author Gavin King, Simon Harris
 */
public class ANSICaseFragment extends CaseFragment {

	public String toFragmentString() {
		
		StringBuffer buf = new StringBuffer( cases.size() * 15 + 10 )
			.append("case");

		Iterator iter = cases.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			buf.append(" when ")
				.append( me.getKey() )
				.append(" is not null then ")
				.append( me.getValue() );
		}
		
		buf.append(" end");

		if (returnColumnName!=null) {
			buf.append(" as ")
				.append(returnColumnName);
		}

		return buf.toString();
	}
	
}