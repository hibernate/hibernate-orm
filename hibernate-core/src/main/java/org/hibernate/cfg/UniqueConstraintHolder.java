/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;


/**
 * {@link javax.persistence.UniqueConstraint} annotations are handled via second pass.  I do not
 * understand the reasons why at this time, so here I use a holder object to hold the information
 * needed to create the unique constraint.  The ability to name it is new, and so the code used to
 * simply keep this as a String array (the column names).
 *
 * Isn't this ultimately the same as org.hibernate.cfg.IndexOrUniqueKeySecondPass?
 *
 * @author Steve Ebersole
 */
public class UniqueConstraintHolder {
	private String name;
	private String[] columns;

	public String getName() {
		return name;
	}

	public UniqueConstraintHolder setName(String name) {
		this.name = name;
		return this;
	}

	public String[] getColumns() {
		return columns;
	}

	public UniqueConstraintHolder setColumns(String[] columns) {
		this.columns = columns;
		return this;
	}
}
