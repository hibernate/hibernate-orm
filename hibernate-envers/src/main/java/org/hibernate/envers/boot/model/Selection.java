/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

/**
 * A contract for a selection, can be a column or a formula.
 *
 * @author Chris Cranford
 */
public abstract class Selection<T> implements Bindable<T> {

	public enum SelectionType {
		COLUMN,
		FORMULA
	}

	private final SelectionType selectionType;

	public Selection(SelectionType selectionType) {
		this.selectionType = selectionType;
	}

	public SelectionType getSelectionType() {
		return selectionType;
	}

}
