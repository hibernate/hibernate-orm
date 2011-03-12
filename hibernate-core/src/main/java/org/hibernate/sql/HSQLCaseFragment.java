/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.sql;

import java.util.Iterator;
import java.util.Map;

/**
 * The HSQL CASEWHEN function.
 * <br>
 * <code>casewhen(..., ..., ...) as ...</code>
 * <br>
 * @author Wolfgang Jung
 */
public class HSQLCaseFragment extends CaseFragment {

	public String toFragmentString() {
		StringBuffer buf = new StringBuffer( cases.size() * 15 + 10 );
		StringBuffer buf2 = new StringBuffer( cases.size() );

		Iterator iter = cases.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			buf.append(" casewhen(")
				.append( me.getKey() )
				.append(" is not null")
				.append(", ")
				.append( me.getValue() )
				.append(", ");
			buf2.append(")");
		}

		buf.append("-1"); //null caused some problems
		buf.append( buf2.toString() );
		if ( returnColumnName!=null ) {
			buf.append(" as ")
				.append(returnColumnName);
		}
		return buf.toString();
	}
}

