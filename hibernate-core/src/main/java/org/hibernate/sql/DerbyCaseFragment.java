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
 * @author Simon Johnston
 * @see org.hibernate.dialect.DerbyDialect
 */
public class DerbyCaseFragment extends CaseFragment {

	/**
	 * From http://www.jroller.com/comments/kenlars99/Weblog/cloudscape_soon_to_be_derby
	 * <p/>
	 * The problem we had, was when Hibernate does a select with a case statement, for joined subclasses.
	 * This seems to be because there was no else at the end of the case statement (other dbs seem to not mind).
	 */
	public String toFragmentString() {
		StringBuilder buf = new StringBuilder( cases.size() * 15 + 10 );
		buf.append( "case" ); 								//$NON-NLS-1
		Iterator iter = cases.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = ( Map.Entry ) iter.next();
			buf.append( " when " )//$NON-NLS-1
					.append( me.getKey() )
					.append( " is not null then " )//$NON-NLS-1
					.append( me.getValue() );
		}
		// null is not considered the same type as Integer.
		buf.append( " else -1" );								//$NON-NLS-1
		buf.append( " end" );									//$NON-NLS-1
		if ( returnColumnName != null ) {
			buf.append( " as " )//$NON-NLS-1
					.append( returnColumnName );
		}
		return buf.toString();
	}
}
