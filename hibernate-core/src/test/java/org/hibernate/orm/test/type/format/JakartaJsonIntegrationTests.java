package org.hibernate.orm.test.type.format;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.hibernate.type.format.jakartajson.JakartaJsonIntegration;
import org.hibernate.type.format.jakartajson.JsonBJsonFormatMapper;

import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.spi.JsonbProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JakartaJsonIntegrationTests {

	@TempDir
	Path serviceDirectory;

	private ClassLoader originalContextClassLoader;

	@BeforeEach
	void rememberContextClassLoader() {
		originalContextClassLoader = Thread.currentThread().getContextClassLoader();
	}

	@AfterEach
	void restoreContextClassLoader() {
		Thread.currentThread().setContextClassLoader( originalContextClassLoader );
	}

	@Test
	void noJsonbApi() throws Exception {
		try ( var classLoader = classLoader( false, false ) ) {
			assertNull( mapper( classLoader ) );
		}
	}

	@Test
	void jsonbApiWithoutProvider() throws Exception {
		try ( var classLoader = classLoader( true, false ) ) {
			assertNull( mapper( classLoader ) );
		}
	}

	@Test
	void explicitJsonbMapperWithoutProvider() throws Exception {
		try ( var classLoader = classLoader( true, false ) ) {
			final var constructor = classLoader.loadClass( JsonBJsonFormatMapper.class.getName() ).getConstructor();
			final var exception = assertThrows( InvocationTargetException.class, constructor::newInstance );
			assertJsonbException( exception.getCause(), classLoader );
		}
	}

	@Test
	void installedProvider() {
		final var mapper = JakartaJsonIntegration.getJakartaJsonBFormatMapperOrNull();
		assertNotNull( mapper );
		assertInstanceOf( JsonBJsonFormatMapper.class, mapper );
	}

	@Test
	void brokenProvider() throws Exception {
		final var serviceFile = serviceDirectory.resolve( "META-INF/services/" + JsonbProvider.class.getName() );
		Files.createDirectories( serviceFile.getParent() );
		Files.writeString( serviceFile, BrokenProvider.class.getName() );
		try ( var classLoader = classLoader( true, true ) ) {
			final var exception = assertThrows( ExceptionInInitializerError.class, () -> mapper( classLoader ) );
			assertJsonbException( exception.getCause(), classLoader );
			assertInstanceOf( ClassNotFoundException.class, exception.getCause().getCause() );
		}
	}

	private static Object mapper(ClassLoader classLoader) throws Exception {
		return classLoader.loadClass( JakartaJsonIntegration.class.getName() )
				.getMethod( "getJakartaJsonBFormatMapperOrNull" ).invoke( null );
	}

	private static void assertJsonbException(Throwable exception, ClassLoader classLoader) throws Exception {
		assertInstanceOf( classLoader.loadClass( JsonbException.class.getName() ), exception );
	}

	private URLClassLoader classLoader(boolean jsonbApi, boolean brokenProvider) throws Exception {
		final var urls = new ArrayList<URL>();
		urls.add( JakartaJsonIntegration.class.getProtectionDomain().getCodeSource().getLocation() );
		if ( jsonbApi ) {
			urls.add( JsonbBuilder.class.getProtectionDomain().getCodeSource().getLocation() );
		}
		if ( brokenProvider ) {
			urls.add( serviceDirectory.toUri().toURL() );
			urls.add( BrokenProvider.class.getProtectionDomain().getCodeSource().getLocation() );
		}
		// Isolate static initialization and exclude providers from the test runtime classpath.
		final var classLoader = new URLClassLoader( urls.toArray( URL[]::new ), ClassLoader.getPlatformClassLoader() );
		Thread.currentThread().setContextClassLoader( classLoader );
		return classLoader;
	}

	public static class BrokenProvider extends JsonbProvider {
		@Override
		public JsonbBuilder create() {
			throw new JsonbException( "Broken provider", new ClassNotFoundException( "Missing provider dependency" ) );
		}
	}
}
