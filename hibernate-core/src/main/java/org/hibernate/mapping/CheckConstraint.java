/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

/**
 * Represents a table or column level {@code check} constraint.
 *
 * @author Gavin King
 */
public class CheckConstraint {
	private String name;
	private String constraint;

	public CheckConstraint(String name, String constraint) {
		this.name = name;
		this.constraint = constraint;
	}

	public CheckConstraint(String constraint) {
		this.constraint = constraint;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getConstraint() {
		return constraint;
	}

	public void setConstraint(String constraint) {
		this.constraint = constraint;
	}

	public String constraintString() {
		return name == null
				? " check (" + constraint + ")"
				: " constraint " + name + " check (" + constraint + ")";
	}
}
