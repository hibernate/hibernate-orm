/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.internal.util.StringHelper;

/**
 * Models an identifier (name), retrieved from the database.
 *
 * @author Andrea Boriero
 */
public class DatabaseIdentifier extends Identifier {
	protected DatabaseIdentifier(String text) {
		super( text );
	}

	/**
	 * Constructs a datatabase identifier instance.
	 *
	 * @param text The identifier text.
	 */
	public static DatabaseIdentifier toIdentifier(String text) {
		if ( StringHelper.isEmpty( text ) ) {
			return null;
		}
		else if ( isQuoted( text ) ) {
			// exclude the quotes from text
			final String unquotedtext = text.substring( 1, text.length() - 1 );
			return new DatabaseIdentifier( unquotedtext );
		}
		else {
			return new DatabaseIdentifier( text );
		}
	}
}
