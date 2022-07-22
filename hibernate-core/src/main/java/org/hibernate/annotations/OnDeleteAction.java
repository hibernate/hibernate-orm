/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Possible actions for on-delete.
 *
 * @author Emmanuel Bernard
 */
public enum OnDeleteAction {
	/**
	 * Take no action.  The default.
	 */
	NO_ACTION( "no-action" ),
	/**
	 * Use cascade delete capabilities of the database foreign-key.
	 */
	CASCADE( "cascade" );

	private final String alternativeName;

	OnDeleteAction(String alternativeName) {
		this.alternativeName = alternativeName;
	}

	public String getAlternativeName() {
		return alternativeName;
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
			if ( checkAction.alternativeName.equalsIgnoreCase( valueString ) ) {
				return checkAction;
			}
		}

		return null;
	}
}
