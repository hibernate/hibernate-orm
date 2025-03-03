/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jakartajson;

import org.hibernate.type.format.FormatMapper;

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
