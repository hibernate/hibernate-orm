//$Id: MckoiCaseFragment.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.sql;

import java.util.Iterator;
import java.util.Map;

/**
 * A Mckoi IF function.
 * <br>
 * <code>if(..., ..., ...) as ...</code>
 * <br>
 * @author Gavin King
 */
public class MckoiCaseFragment extends CaseFragment {

	public String toFragmentString() {
		StringBuffer buf = new StringBuffer( cases.size() * 15 + 10 );
		StringBuffer buf2= new StringBuffer( cases.size() );

		Iterator iter = cases.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			buf.append(" if(")
				.append( me.getKey() )
				.append(" is not null")
				.append(", ")
				.append( me.getValue() )
				.append(", ");
			buf2.append(")");
		}

		buf.append("null");
		buf.append(buf2);
		if (returnColumnName!=null) {
			buf.append(" as ")
				.append(returnColumnName);
		}

		return buf.toString();
	}
}

