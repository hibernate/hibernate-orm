/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import org.hibernate.type.format.FormatMapper;

public final class JacksonIntegration {

	// Implementation note: we rely on the following fields to be folded as constants
	// when GraalVM native image is initializing them.
	private static final boolean JACKSON_XML_AVAILABLE = ableToLoadJacksonXMLMapper();
	private static final boolean JACKSON_JSON_AVAILABLE = ableToLoadJacksonJSONMapper();
	private static final JacksonXmlFormatMapper XML_FORMAT_MAPPER = JACKSON_XML_AVAILABLE ? new JacksonXmlFormatMapper() : null;
	private static final JacksonXmlFormatMapper XML_FORMAT_MAPPER_PORTABLE = JACKSON_XML_AVAILABLE ? new JacksonXmlFormatMapper( false ) : null;
	private static final JacksonJsonFormatMapper JSON_FORMAT_MAPPER = JACKSON_JSON_AVAILABLE ? new JacksonJsonFormatMapper() : null;

	private JacksonIntegration() {
		//To not be instantiated: static helpers only
	}

	private static boolean ableToLoadJacksonJSONMapper() {
		return canLoad( "com.fasterxml.jackson.databind.ObjectMapper" );
	}

	private static boolean ableToLoadJacksonXMLMapper() {
		return canLoad( "com.fasterxml.jackson.dataformat.xml.XmlMapper" );
	}

	public static FormatMapper getXMLJacksonFormatMapperOrNull() {
		return XML_FORMAT_MAPPER;
	}

	public static FormatMapper getXMLJacksonFormatMapperOrNull(boolean legacyFormat) {
		return legacyFormat ? XML_FORMAT_MAPPER : XML_FORMAT_MAPPER_PORTABLE;
	}

	public static FormatMapper getJsonJacksonFormatMapperOrNull() {
		return JSON_FORMAT_MAPPER;
	}

	private static boolean canLoad(String name) {
		try {
			//N.B. intentionally not using the context classloader
			// as we're storing these in static references;
			// IMO it's reasonable to expect that such dependencies are made reachable from the ORM classloader.
			// (we can change this if it's more problematic than expected).
			JacksonIntegration.class.getClassLoader().loadClass( name );
			return true;
		}
		catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}
}
