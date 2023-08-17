/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;


import jakarta.persistence.UniqueConstraint;

/**
 * {@link jakarta.persistence.UniqueConstraint} annotations are handled via second pass. I do not
 * understand the reasons why at this time, so here I use a holder object to hold the information
 * needed to create the unique constraint. The ability to name it is new, and so the code used to
 * simply keep this as a String array (the column names).
 *
 * @author Steve Ebersole
 */
// Isn't this ultimately the same as IndexOrUniqueKeySecondPass?
public class UniqueConstraintHolder {
	private final String name;
	private final String[] columns;

	public String getName() {
		return name;
	}

	public UniqueConstraintHolder(UniqueConstraint uniqueConstraint) {
		this.name = uniqueConstraint.name();
		this.columns = uniqueConstraint.columnNames();
	}

	public UniqueConstraintHolder(String name, String[] columns) {
		this.name = name;
		this.columns = columns;
	}

	public boolean isNameExplicit() {
		return true;
	}

	public String[] getColumns() {
		return columns;
	}
}
