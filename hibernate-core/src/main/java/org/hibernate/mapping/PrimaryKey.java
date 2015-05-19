/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import java.util.Iterator;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * A primary key constraint
 * @author Gavin King
 */
public class PrimaryKey extends Constraint {

	public String sqlConstraintString(Dialect dialect) {
		StringBuilder buf = new StringBuilder("primary key (");
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) {
				buf.append(", ");
			}
		}
		return buf.append(')').toString();
	}

	public String sqlConstraintString(Dialect dialect, String constraintName, String defaultCatalog, String defaultSchema) {
		StringBuilder buf = new StringBuilder(
			dialect.getAddPrimaryKeyConstraintString(constraintName)
		).append('(');
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) {
				buf.append(", ");
			}
		}
		return buf.append(')').toString();
	}
	
	public String generatedConstraintNamePrefix() {
		return "PK_";
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getName(), "PK-" + getName() );
	}
}
