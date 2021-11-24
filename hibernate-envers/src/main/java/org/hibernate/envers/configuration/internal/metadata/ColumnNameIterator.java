/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Iterator;

import org.hibernate.mapping.Selectable;

/**
 * Utility class to build a column name iterator over different column collection types.
 *
 * @author Chris Cranford
 */
public abstract class ColumnNameIterator implements Iterator<String> {

	public static ColumnNameIterator from(Iterator<Selectable> selectables) {
		return new ColumnNameIterator() {
			public boolean hasNext() {
				return selectables.hasNext();
			}

			public String next() {
				final Selectable next = selectables.next();
				if ( next.isFormula() ) {
					throw new FormulaNotSupportedException();
				}
				return ( (org.hibernate.mapping.Column) next ).getName();
			}

			public void remove() {
				selectables.remove();
			}
		};
	}

}
