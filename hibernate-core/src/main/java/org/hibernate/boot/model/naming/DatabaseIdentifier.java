/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Models an identifier (name), retrieved from the database.
 *
 * @author Andrea Boriero
 */
public class DatabaseIdentifier extends Identifier {

	/**
	 * Constructs a database identifier instance.
	 * It is assumed that <code>text</code> is unquoted.
	 *
	 * @param text The identifier text.
	 */
	protected DatabaseIdentifier(String text) {
		super( text );
	}

	public static DatabaseIdentifier toIdentifier(String text) {
		if ( isEmpty( text ) ) {
			return null;
		}
		else if ( isQuoted( text ) ) {
			// exclude the quotes from text
			final String unquoted = text.substring( 1, text.length() - 1 );
			return new DatabaseIdentifier( unquoted );
		}
		else {
			return new DatabaseIdentifier( text );
		}
	}
}
