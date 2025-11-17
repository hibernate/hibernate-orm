/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import static java.util.Locale.ROOT;

/**
 * Enumerates the possible actions for the {@code on delete} clause
 * of a foreign key constraint. Specifies what action the database
 * should take when deletion of a row would result in a violation of
 * the constraint.
 *
 * @author Emmanuel Bernard
 *
 * @see OnDelete
 */
public enum OnDeleteAction {

	/**
	 * No action. The default. An error is raised if rows still reference
	 * the parent when the constraint is checked, possibly later in the
	 * transaction.
	 */
	NO_ACTION,

	/**
	 * Cascade deletion of the parent to the child.
	 * <p>
	 * Produces a foreign key constraint with {@code on delete cascade}.
	 */
	CASCADE,

	/**
	 * Prevents deletion of the parent by raising an error immediately.
	 * <p>
	 * Produces a foreign key constraint with {@code on delete restrict}.
	 *
	 * @since 6.2
	 */
	RESTRICT,

	/**
	 * Set the referencing foreign key to null.
	 * <p>
	 * Produces a foreign key constraint with {@code on delete set null}.
	 *
	 * @since 6.2
	 */
	SET_NULL,

	/**
	 * Set the referencing foreign key to its default value.
	 * <p>
	 * Produces a foreign key constraint with {@code on delete set default}.
	 *
	 * @since 6.2
	 */
	SET_DEFAULT;

	public String getAlternativeName() {
		return toString().toLowerCase(ROOT).replace('_', '-');
	}

	public String toSqlString() {
		return toString().toLowerCase(ROOT).replace('_', ' ');
	}

	public static OnDeleteAction fromExternalForm(Object value) {
		if ( value == null ) {
			return null;
		}
		else if ( value instanceof OnDeleteAction onDeleteAction ) {
			return onDeleteAction;
		}
		else {
			final String valueString = value.toString();
			try {
				return valueOf( valueString );
			}
			catch (IllegalArgumentException e) {
				// the name did not match the enum value name...
			}

			for ( OnDeleteAction checkAction : values() ) {
				if ( checkAction.getAlternativeName().equalsIgnoreCase( valueString ) ) {
					return checkAction;
				}
			}

			return null;
		}
	}
}
