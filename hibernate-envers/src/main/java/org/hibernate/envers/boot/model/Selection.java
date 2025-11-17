/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
