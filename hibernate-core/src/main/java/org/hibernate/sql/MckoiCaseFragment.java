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
 * A Mckoi IF function.
 * <br>
 * <code>if(..., ..., ...) as ...</code>
 * <br>
 * @author Gavin King
 */
public class MckoiCaseFragment extends CaseFragment {

	public String toFragmentString() {
		StringBuilder buf = new StringBuilder( cases.size() * 15 + 10 );
		StringBuilder buf2= new StringBuilder( cases.size() );

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

