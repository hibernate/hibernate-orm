//$Id: DecodeCaseFragment.java 4851 2004-12-02 05:09:49Z oneovthafew $
package org.hibernate.sql;

import java.util.Iterator;
import java.util.Map;

/**
 An Oracle-style DECODE function.
 <br>
 <code>decode(pkvalue, key1, 1, key2, 2, ..., 0)</code>
 <br>

 @author Simon Harris
 */
public class DecodeCaseFragment extends CaseFragment {

	public String toFragmentString() {
		
		StringBuffer buf = new StringBuffer( cases.size() * 15 + 10 )
			.append("decode(");

		Iterator iter = cases.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();

			if ( iter.hasNext() ) {
				buf.append(", ")
					.append( me.getKey() )
					.append(", ")
					.append( me.getValue() );
			}
			else {
				buf.insert( 7, me.getKey() )
					.append(", ")
					.append( me.getValue() );
			}
		}

		buf.append(')');
		
		if (returnColumnName!=null) {
			buf.append(" as ")
				.append(returnColumnName);
		}
		
		return buf.toString();
	}
}