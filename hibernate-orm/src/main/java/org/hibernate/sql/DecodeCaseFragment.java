/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;
import java.util.Iterator;
import java.util.Map;

/**
 * An Oracle-style DECODE function.
 * <br>
 * <code>decode(pkvalue, key1, 1, key2, 2, ..., 0)</code>
 *
 * @author Simon Harris
 */
public class DecodeCaseFragment extends CaseFragment {

	public String toFragmentString() {
		
		StringBuilder buf = new StringBuilder( cases.size() * 15 + 10 )
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
