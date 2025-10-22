/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.format.jackson;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.FormatMapperCreationContext;

public final class JacksonIntegration {

	// Implementation note: we rely on the following fields to be folded as constants
	// when GraalVM native image is initializing them.
	private static final boolean JACKSON_XML_AVAILABLE = ableToLoadJacksonXMLMapper();
	private static final boolean JACKSON_JSON_AVAILABLE = ableToLoadJacksonJSONMapper();


	private JacksonIntegration() {
		//To not be instantiated: static helpers only
	}

	private static boolean ableToLoadJacksonJSONMapper() {
		return canLoad( "com.fasterxml.jackson.databind.ObjectMapper" );
	}

	private static boolean ableToLoadJacksonXMLMapper() {
		return canLoad( "com.fasterxml.jackson.dataformat.xml.XmlMapper" );
	}

	/**
	 * Checks that Jackson is available and that we have the Oracle OSON extension available
	 * in the classpath.
	 * @return true if we can load the OSON support, false otherwise.
	 */
	private static boolean ableToLoadJacksonOSONFactory() {
		return ableToLoadJacksonJSONMapper() &&
				canLoad( "oracle.jdbc.provider.oson.OsonFactory" );
	}

	public static @Nullable FormatMapper getXMLJacksonFormatMapperOrNull(FormatMapperCreationContext creationContext) {
		return JACKSON_XML_AVAILABLE
				? createFormatMapper( "org.hibernate.type.format.jackson.JacksonXmlFormatMapper", creationContext )
				: null;
	}

	public static @Nullable FormatMapper getJsonJacksonFormatMapperOrNull(FormatMapperCreationContext creationContext) {
		return JACKSON_JSON_AVAILABLE
				? createFormatMapper( "org.hibernate.type.format.jackson.JacksonJsonFormatMapper", creationContext )
				: null;
	}

	public static @Nullable FormatMapper getXMLJacksonFormatMapperOrNull(boolean legacyFormat) {
		if ( JACKSON_XML_AVAILABLE ) {
			try {
				final Class<?> formatMapperClass = JacksonIntegration.class.getClassLoader()
						.loadClass( "org.hibernate.type.format.jackson.JacksonXmlFormatMapper" );
				return (FormatMapper) formatMapperClass.getDeclaredConstructor( boolean.class )
						.newInstance( legacyFormat );
			}
			catch (Exception e) {
				throw new RuntimeException( "Couldn't instantiate Jackson XML FormatMapper", e );
			}
		}
		return null;
	}

	public static @Nullable FormatMapper getJsonJacksonFormatMapperOrNull() {
		return JACKSON_JSON_AVAILABLE
				? createFormatMapper( "org.hibernate.type.format.jackson.JacksonJsonFormatMapper", null )
				: null;
	}

	private static FormatMapper createFormatMapper(String className, @Nullable FormatMapperCreationContext creationContext) {
		try {
			if ( creationContext == null ) {
				final Class<?> formatMapperClass = JacksonIntegration.class.getClassLoader()
						.loadClass( className );
				return (FormatMapper) formatMapperClass.getDeclaredConstructor().newInstance();
			}
			else {
				return (FormatMapper) creationContext.getBootstrapContext()
						.getClassLoaderAccess()
						.classForName( className )
						.getDeclaredConstructor( FormatMapperCreationContext.class )
						.newInstance( creationContext );
			}
		}
		catch (Exception e) {
			throw new RuntimeException( "Couldn't instantiate Jackson FormatMapper", e );
		}
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
