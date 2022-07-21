/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.jackson;

import org.hibernate.type.FormatMapper;
import org.hibernate.type.JacksonJsonFormatMapper;
import org.hibernate.type.JacksonXmlFormatMapper;

public final class JacksonIntegration {

	// Implementation note: we rely on the following two fields to be folded as constants
	// when GraalVM native image is initializing them.
	private static final boolean JACKSON_AVAILABLE = ableToLoadJackson();
	private static final JacksonXmlFormatMapper XML_FORMAT_MAPPER = JACKSON_AVAILABLE ? new JacksonXmlFormatMapper() : null;
	private static final JacksonJsonFormatMapper JSON_FORMAT_MAPPER = JACKSON_AVAILABLE ? new JacksonJsonFormatMapper() : null;

	private JacksonIntegration() {
		//To not be instantiated: static helpers only
	}

	private static boolean ableToLoadJackson() {
		try {
			JacksonIntegration.class.getClassLoader().loadClass( "com.fasterxml.jackson.dataformat.xml.XmlMapper" );
			return true;
		}
		catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}

	public static FormatMapper getXMLJacksonFormatMapperOrNull() {
		return XML_FORMAT_MAPPER;
	}

	public static FormatMapper getJsonJacksonFormatMapperOrNull() {
		return JSON_FORMAT_MAPPER;
	}
}
