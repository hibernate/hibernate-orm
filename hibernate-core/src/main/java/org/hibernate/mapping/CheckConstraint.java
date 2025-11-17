/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.Objects;

import org.hibernate.dialect.Dialect;

import static org.hibernate.internal.util.StringHelper.isBlank;

/**
 * Represents a table or column level {@code check} constraint.
 *
 * @author Gavin King
 */
public class CheckConstraint {
	private String name;
	private String constraint;
	private String options;

	public CheckConstraint(String name, String constraint) {
		this.name = name;
		this.constraint = constraint;
	}

	public CheckConstraint(String name, String constraint, String options) {
		this.name = name;
		this.constraint = constraint;
		this.options = options;
	}

	public CheckConstraint(String constraint) {
		this.constraint = constraint;
	}

	public boolean isNamed() {
		return name != null;
	}

	public boolean isAnonymous() {
		return name == null;
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

	public String getConstraintInParens() {
		return "(" + constraint + ")";
	}

	public void setConstraint(String constraint) {
		this.constraint = constraint;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	/**
	 * @deprecated use {@link #constraintString(Dialect)} instead.
	 */
	@Deprecated(since = "7.0")
	public String constraintString() {
		return isBlank( name )
				? " check (" + constraint + ")"
				: " constraint " + name + " check (" + constraint + ")";
	}

	public String constraintString(Dialect dialect) {
		return dialect.getCheckConstraintString( this );
	}

	@Override
	public boolean equals(Object object) {
		if ( object instanceof CheckConstraint other ) {
			return Objects.equals( name, other.name )
				&& Objects.equals( constraint, other.constraint );
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return constraint.hashCode();
	}
}
