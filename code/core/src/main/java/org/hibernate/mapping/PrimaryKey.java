//$Id: PrimaryKey.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.dialect.Dialect;

/**
 * A primary key constraint
 * @author Gavin King
 */
public class PrimaryKey extends Constraint {

	public String sqlConstraintString(Dialect dialect) {
		StringBuffer buf = new StringBuffer("primary key (");
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) buf.append(", ");
		}
		return buf.append(')').toString();
	}

	public String sqlConstraintString(Dialect dialect, String constraintName, String defaultCatalog, String defaultSchema) {
		StringBuffer buf = new StringBuffer(
			dialect.getAddPrimaryKeyConstraintString(constraintName)
		).append('(');
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) buf.append(", ");
		}
		return buf.append(')').toString();
	}
}
