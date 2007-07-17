// $Id: NameGenerator.java 4899 2004-12-06 14:17:24Z pgmjsd $
package org.hibernate.hql;

import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Provides utility methods for generating HQL / SQL names.   Shared by both the 'classic' and 'new' query translators.
 *
 * @author josh Mar 18, 2004 7:17:25 AM
 */
public final class NameGenerator {
	/**
	 * Private empty constructor (checkstyle says utility classes should not have default constructors).
	 */
	private NameGenerator() {
	}

	public static String[][] generateColumnNames(Type[] types, SessionFactoryImplementor f) throws MappingException {
		String[][] columnNames = new String[types.length][];
		for ( int i = 0; i < types.length; i++ ) {
			int span = types[i].getColumnSpan( f );
			columnNames[i] = new String[span];
			for ( int j = 0; j < span; j++ ) {
				columnNames[i][j] = NameGenerator.scalarName( i, j );
			}
		}
		return columnNames;
	}

	public static String scalarName(int x, int y) {
		return new StringBuffer()
				.append( "col_" )
				.append( x )
				.append( '_' )
				.append( y )
				.append( '_' )
				.toString();
	}
}
