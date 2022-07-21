/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.jakartajson;

import org.hibernate.type.FormatMapper;
import org.hibernate.type.JsonBJsonFormatMapper;

public final class JakartaJsonIntegration {

	// Implementation note: we rely on the following two fields to be folded as constants
	// when GraalVM native image is initializing them.
	private static final boolean JAKARTA_JSON_AVAILABLE = ableToLoadJakartaJsonB();
	private static final JsonBJsonFormatMapper JSON_FORMAT_MAPPER = JAKARTA_JSON_AVAILABLE ? new JsonBJsonFormatMapper() : null;

	private JakartaJsonIntegration() {
		//To not be instantiated: static helpers only
	}

	private static boolean ableToLoadJakartaJsonB() {
		try {
			JakartaJsonIntegration.class.getClassLoader().loadClass( "jakarta.json.bind.Jsonb" );
			return true;
		}
		catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}

	public static FormatMapper getJakartaJsonBFormatMapperOrNull() {
		return JSON_FORMAT_MAPPER;
	}

}
