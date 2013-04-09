/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
