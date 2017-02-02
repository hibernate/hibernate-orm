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
	/**
	 * Constructs a datatabase identifier instance.
	 *
	 * @param text The identifier text.
	 */
	public DatabaseIdentifier(String text) {
		super( text, false );
	}

	public static DatabaseIdentifier toIdentifier(String text) {
		if ( StringHelper.isEmpty( text ) ) {
			return null;
		}
		return new DatabaseIdentifier( text );
	}
}
