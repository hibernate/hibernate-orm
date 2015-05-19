/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import java.util.Map;

/**
 * An ANSI SQL CASE expression : {@code case when ... then ... end as ..}
 *
 * @author Gavin King
 * @author Simon Harris
 */
public class ANSICaseFragment extends CaseFragment {

	@Override
	public String toFragmentString() {
		
		final StringBuilder buf = new StringBuilder( cases.size() * 15 + 10 )
			.append("case");

		for ( Object o : cases.entrySet() ) {
			Map.Entry me = (Map.Entry) o;
			buf.append( " when " )
					.append( me.getKey() )
					.append( " is not null then " )
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
