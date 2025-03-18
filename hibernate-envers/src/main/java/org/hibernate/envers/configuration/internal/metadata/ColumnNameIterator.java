/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
