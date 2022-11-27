/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.AssertionFailure;

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
	 * No action. The default.
	 */
	NO_ACTION,

	/**
	 * Cascade deletion of the parent to the child.
	 */
	CASCADE;

	public String getAlternativeName() {
		switch (this) {
			case NO_ACTION:
				return "no-action";
			case CASCADE:
				return "cascade";
			default:
				throw new AssertionFailure("unknown action");
		}
	}

	public static OnDeleteAction fromExternalForm(Object value) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof OnDeleteAction ) {
			return (OnDeleteAction) value;
		}

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
