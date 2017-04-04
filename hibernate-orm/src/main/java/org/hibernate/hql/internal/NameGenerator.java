/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Provides utility methods for generating HQL / SQL names.   Shared by both the 'classic' and 'new' query translators.
 *
 * @author josh
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
		return scalarName( "col_" + x, y );
	}

	public static String scalarName(String base, int num) {
		return base + '_' + num + '_';
	}

	public static String[] scalarNames(String base, int count) {
		final String[] names = new String[count];
		for ( int j = 0; j < count; j++ ) {
			names[j] = scalarName( base, j );
		}
		return names;
	}

	public static String[] scalarNames(int uniqueness, int count) {
		return scalarNames( "col_" + uniqueness, count );
	}
}
