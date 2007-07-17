//$Id: UniqueKey.java 10661 2006-10-31 02:19:13Z epbernard $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.util.StringHelper;

/**
 * A relational unique key constraint
 *
 * @author Gavin King
 */
public class UniqueKey extends Constraint {

	public String sqlConstraintString(Dialect dialect) {
		StringBuffer buf = new StringBuffer( "unique (" );
		Iterator iter = getColumnIterator();
		boolean nullable = false;
		while ( iter.hasNext() ) {
			Column column = (Column) iter.next();
			if ( !nullable && column.isNullable() ) nullable = true;
			buf.append( column.getQuotedName( dialect ) );
			if ( iter.hasNext() ) buf.append( ", " );
		}
		//do not add unique constraint on DB not supporting unique and nullable columns
		return !nullable || dialect.supportsNotNullUnique() ?
				buf.append( ')' ).toString() :
				null;
	}

	public String sqlConstraintString(Dialect dialect, String constraintName, String defaultCatalog,
									  String defaultSchema) {
		StringBuffer buf = new StringBuffer(
				dialect.getAddPrimaryKeyConstraintString( constraintName )
		).append( '(' );
		Iterator iter = getColumnIterator();
		boolean nullable = false;
		while ( iter.hasNext() ) {
			Column column = (Column) iter.next();
			if ( !nullable && column.isNullable() ) nullable = true;
			buf.append( column.getQuotedName( dialect ) );
			if ( iter.hasNext() ) buf.append( ", " );
		}
		return !nullable || dialect.supportsNotNullUnique() ?
				StringHelper.replace( buf.append( ')' ).toString(), "primary key", "unique" ) :
				//TODO: improve this hack!
				null;
	}

	public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			return super.sqlCreateString( dialect, p, defaultCatalog, defaultSchema );
		}
		else {
			return Index.buildSqlCreateIndexString( dialect, getName(), getTable(), getColumnIterator(), true,
					defaultCatalog, defaultSchema );
		}
	}

	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			return super.sqlDropString( dialect, defaultCatalog, defaultSchema );
		}
		else {
			return Index.buildSqlDropIndexString( dialect, getTable(), getName(), defaultCatalog, defaultSchema );
		}
	}

	public boolean isGenerated(Dialect dialect) {
		if ( dialect.supportsNotNullUnique() ) return true;
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			if ( ( (Column) iter.next() ).isNullable() ) {
				return false;
			}
		}
		return true;
	}

}
