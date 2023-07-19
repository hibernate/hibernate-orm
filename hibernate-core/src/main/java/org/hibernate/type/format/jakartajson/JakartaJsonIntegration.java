/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.format.jakartajson;

import org.hibernate.type.format.FormatMapper;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class JakartaJsonIntegration {

	// Implementation note: we rely on the following two fields to be folded as constants
	// when GraalVM native image is initializing them.
	private static final boolean JAKARTA_JSON_AVAILABLE = ableToLoadJakartaJsonB();
	private static final @Nullable JsonBJsonFormatMapper JSON_FORMAT_MAPPER = JAKARTA_JSON_AVAILABLE ? new JsonBJsonFormatMapper() : null;

	private JakartaJsonIntegration() {
		//To not be instantiated: static helpers only
	}

	private static boolean ableToLoadJakartaJsonB() {
		try {
			//N.B. intentionally not using the context classloader
			// as we're storing these in static references;
			// IMO it's reasonable to expect that such dependencies are made reachable from the ORM classloader.
			// (we can change this if it's more problematic than expected).
			JakartaJsonIntegration.class.getClassLoader().loadClass( "jakarta.json.bind.JsonbBuilder" );
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
