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
