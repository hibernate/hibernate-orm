/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.format.jackson;

import java.util.List;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
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

	public static @Nullable FormatMapper getXMLJacksonFormatMapperOrNull(FormatMapperCreationContext creationContext) {
		return JACKSON_XML_AVAILABLE
				? new JacksonXmlFormatMapper( creationContext )
				: null;
	}

	public static @Nullable FormatMapper getJsonJacksonFormatMapperOrNull(FormatMapperCreationContext creationContext) {
		return JACKSON_JSON_AVAILABLE
				? new JacksonJsonFormatMapper( creationContext )
				: null;
	}

	public static @Nullable FormatMapper getJsonJacksonFormatMapperOrNull() {
		return JACKSON_JSON_AVAILABLE
				? new JacksonJsonFormatMapper()
				: null;
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
							.getServiceRegistry()
							.requireService( ClassLoaderService.class )
							.<List<Module>>workWithClassLoader( ObjectMapper::findModules );
				}
			}
			catch (ClassNotFoundException | LinkageError e) {
				// Ignore if the context/application class loader doesn't know Jackson classes
			}
		}
		return ObjectMapper.findModules( classLoader );
	}
}
