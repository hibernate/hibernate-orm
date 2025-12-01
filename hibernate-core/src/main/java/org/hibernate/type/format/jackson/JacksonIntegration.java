/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import java.util.List;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.checkerframework.checker.nullness.qual.Nullable;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.cfg.MapperBuilder;

import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.FormatMapperCreationContext;

public final class JacksonIntegration {

	// Implementation note: we rely on the following fields to be folded as constants
	// when GraalVM native image is initializing them.
	private static final boolean JACKSON_XML_AVAILABLE = ableToLoadJacksonXMLMapper();
	private static final boolean JACKSON_JSON_AVAILABLE = ableToLoadJacksonJSONMapper();
	private static final boolean JACKSON_OSON_AVAILABLE = ableToLoadJacksonOSONFactory();

	private static final boolean JACKSON3_XML_AVAILABLE = ableToLoadJackson3XMLMapper();
	private static final boolean JACKSON3_JSON_AVAILABLE = ableToLoadJackson3JSONMapper();

	private JacksonIntegration() {
		//To not be instantiated: static helpers only
	}

	private static boolean ableToLoadJacksonJSONMapper() {
		return canLoad( "com.fasterxml.jackson.databind.ObjectMapper" );
	}

	private static boolean ableToLoadJackson3JSONMapper() {
		return canLoad( "tools.jackson.databind.json.JsonMapper" );
	}

	private static boolean ableToLoadJacksonXMLMapper() {
		return canLoad( "com.fasterxml.jackson.dataformat.xml.XmlMapper" );
	}

	private static boolean ableToLoadJackson3XMLMapper() {
		return canLoad( "tools.jackson.dataformat.xml.XmlMapper" );
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
				? new JacksonXmlFormatMapper( creationContext )
				: null;
	}

	public static @Nullable FormatMapper getXMLJackson3FormatMapperOrNull(FormatMapperCreationContext creationContext) {
		return JACKSON3_XML_AVAILABLE
				? new Jackson3XmlFormatMapper( creationContext )
				: null;
	}

	public static @Nullable FormatMapper getJsonJacksonFormatMapperOrNull(FormatMapperCreationContext creationContext) {
		return JACKSON_JSON_AVAILABLE
				? new JacksonJsonFormatMapper( creationContext )
				: null;
	}
	public static @Nullable FormatMapper getJsonJackson3FormatMapperOrNull(FormatMapperCreationContext creationContext) {
		return JACKSON3_JSON_AVAILABLE
				? new Jackson3JsonFormatMapper( creationContext )
				: null;
	}
	public static @Nullable FormatMapper getOsonJacksonFormatMapperOrNull(FormatMapperCreationContext creationContext) {
		return JACKSON_OSON_AVAILABLE
				? new JacksonOsonFormatMapper( creationContext )
				: null;
	}

	public static @Nullable FormatMapper getJsonJacksonFormatMapperOrNull() {
		return JACKSON_JSON_AVAILABLE
				? new JacksonJsonFormatMapper()
				: null;
	}
	public static @Nullable FormatMapper getJsonJackson3FormatMapperOrNull() {
		return JACKSON3_JSON_AVAILABLE
				? new Jackson3JsonFormatMapper()
				: null;
	}
	public static @Nullable FormatMapper getOsonJacksonFormatMapperOrNull() {
		return JACKSON_OSON_AVAILABLE
				? new JacksonOsonFormatMapper()
				: null;
	}

	/**
	 * Checks that Oracle OSON extension available
	 *
	 * @return true if we can load the OSON support, false otherwise.
	 */
	public static boolean isJacksonOsonExtensionAvailable() {
		return JACKSON_OSON_AVAILABLE;
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

	static List<Module> loadModules(FormatMapperCreationContext creationContext) {
		final ClassLoader classLoader = JacksonIntegration.class.getClassLoader();
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		if ( contextClassLoader != null && classLoader != contextClassLoader ) {
			try {
				// The context class loader represents the application class loader in a Jakarta EE deployment.
				// We have to check if the ObjectMapper that is visible to Hibernate ORM is the same that is visible
				// to the application class loader. Only if it is, we can use the application class loader or rather
				// our AggregatedClassLoader for loading Jackson Module via ServiceLoader, as otherwise the loaded
				// Jackson Module instances would have a different class loader, leading to a ClassCastException.
				if ( ObjectMapper.class == contextClassLoader.loadClass( "com.fasterxml.jackson.databind.ObjectMapper" ) ) {
					return creationContext.getBootstrapContext()
							.getClassLoaderService()
							.<List<Module>>workWithClassLoader( ObjectMapper::findModules );
				}
			}
			catch (ClassNotFoundException | LinkageError e) {
				// Ignore if the context/application class loader doesn't know Jackson classes
			}
		}
		return ObjectMapper.findModules( classLoader );
	}

	static List<JacksonModule> loadJackson3Modules(FormatMapperCreationContext creationContext) {
		final ClassLoader classLoader = JacksonIntegration.class.getClassLoader();
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		if ( contextClassLoader != null && classLoader != contextClassLoader ) {
			try {
				// The context class loader represents the application class loader in a Jakarta EE deployment.
				// We have to check if the ObjectMapper that is visible to Hibernate ORM is the same that is visible
				// to the application class loader. Only if it is, we can use the application class loader or rather
				// our AggregatedClassLoader for loading Jackson Module via ServiceLoader, as otherwise the loaded
				// Jackson Module instances would have a different class loader, leading to a ClassCastException.
				if ( ObjectMapper.class == contextClassLoader.loadClass( "tools.jackson.databind.ObjectMapper" ) ) {
					return creationContext.getBootstrapContext()
							.getClassLoaderService()
							.<List<JacksonModule>>workWithClassLoader( MapperBuilder::findModules );
				}
			}
			catch (ClassNotFoundException | LinkageError e) {
				// Ignore if the context/application class loader doesn't know Jackson classes
			}
		}
		return MapperBuilder.findModules( classLoader );
	}
}
