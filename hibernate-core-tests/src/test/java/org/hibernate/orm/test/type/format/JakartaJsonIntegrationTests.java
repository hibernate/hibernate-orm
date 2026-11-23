package org.hibernate.orm.test.type.format;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.type.format.jakartajson.JakartaJsonIntegration;
import org.hibernate.type.format.jakartajson.JsonBJsonFormatMapper;

import org.hibernate.testing.orm.junit.ClassLoadingIsolaterExtension;

import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.spi.JsonbProvider;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JakartaJsonIntegrationTests implements ClassLoadingIsolaterExtension.IsolatedClassLoaderProvider {

	@TempDir
	Path serviceDirectory;

	private boolean jsonbApiAvailable = true;
	private boolean failProviderDiscovery;

	@Test
	@ExtendWith(ClassLoadingIsolaterExtension.class)
	void noJsonbApi() throws Exception {
		jsonbApiAvailable = false;
		assertNull( mapper() );
	}

	@Test
	@ExtendWith(ClassLoadingIsolaterExtension.class)
	void jsonbApiWithoutProvider() throws Exception {
		assertNull( mapper() );
	}

	@Test
	@ExtendWith(ClassLoadingIsolaterExtension.class)
	void explicitJsonbMapperWithoutProvider() throws Exception {
		final var constructor = Thread.currentThread().getContextClassLoader()
				.loadClass( JsonBJsonFormatMapper.class.getName() ).getConstructor();
		final var exception = assertThrows( InvocationTargetException.class, constructor::newInstance );
		assertJsonbException( exception.getCause() );
	}

	@Test
	void installedProvider() {
		final var mapper = JakartaJsonIntegration.getJakartaJsonBFormatMapperOrNull();
		assertNotNull( mapper );
		assertInstanceOf( JsonBJsonFormatMapper.class, mapper );
	}

	@Test
	@ExtendWith(ClassLoadingIsolaterExtension.class)
	void failedProviderDiscovery() throws Exception {
		failProviderDiscovery = true;
		final var providerMethod = Thread.currentThread().getContextClassLoader()
				.loadClass( JsonbProvider.class.getName() ).getMethod( "provider" );
		final var exception = assertThrows( InvocationTargetException.class, () -> providerMethod.invoke( null ) );
		assertJsonbException( exception.getCause() );
		assertInstanceOf( IllegalStateException.class, exception.getCause().getCause() );
		assertNull( mapper() );
	}

	@Test
	@ExtendWith(ClassLoadingIsolaterExtension.class)
	void brokenProvider() throws Exception {
		final var serviceFile = serviceDirectory.resolve( "META-INF/services/" + JsonbProvider.class.getName() );
		Files.createDirectories( serviceFile.getParent() );
		Files.writeString( serviceFile, BrokenProvider.class.getName() );
		final var exception = assertThrows( ExceptionInInitializerError.class, JakartaJsonIntegrationTests::mapper );
		assertJsonbException( exception.getCause() );
		assertInstanceOf( ClassNotFoundException.class, exception.getCause().getCause() );
	}

	@Override
	public ClassLoader buildIsolatedClassLoader() {
		// Isolate static initialization and exclude providers from the test runtime classpath.
		return new URLClassLoader( new URL[] {
				JakartaJsonIntegration.class.getProtectionDomain().getCodeSource().getLocation(),
				JsonbBuilder.class.getProtectionDomain().getCodeSource().getLocation(),
				Logger.class.getProtectionDomain().getCodeSource().getLocation(),
				BrokenProvider.class.getProtectionDomain().getCodeSource().getLocation(),
				assertDoesNotThrow( () -> serviceDirectory.toUri().toURL() )
		}, ClassLoader.getPlatformClassLoader() ) {
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				if ( !jsonbApiAvailable && name.startsWith( "jakarta.json.bind." ) ) {
					throw new ClassNotFoundException( name );
				}
				if ( failProviderDiscovery && name.equals( "org.eclipse.yasson.JsonBindingProvider" ) ) {
					throw new IllegalStateException( "Provider discovery failed" );
				}
				return super.loadClass( name );
			}
		};
	}

	@Override
	public void releaseIsolatedClassLoader(ClassLoader isolatedClassLoader) {
		assertDoesNotThrow( () -> ( (URLClassLoader) isolatedClassLoader ).close() );
	}

	private static Object mapper() throws Exception {
		return Thread.currentThread().getContextClassLoader().loadClass( JakartaJsonIntegration.class.getName() )
				.getMethod( "getJakartaJsonBFormatMapperOrNull" ).invoke( null );
	}

	private static void assertJsonbException(Throwable exception) throws Exception {
		assertInstanceOf( Thread.currentThread().getContextClassLoader().loadClass( JsonbException.class.getName() ), exception );
	}

	public static class BrokenProvider extends JsonbProvider {
		@Override
		public JsonbBuilder create() {
			throw new JsonbException( "Broken provider", new ClassNotFoundException( "Missing provider dependency" ) );
		}
	}
}
