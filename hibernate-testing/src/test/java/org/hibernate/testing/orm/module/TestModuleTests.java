package org.hibernate.testing.orm.module;

import java.net.URLConnection;
import java.nio.file.Path;

import org.hibernate.testing.orm.module.fixture.ModuleFixture;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// @author Steve Ebersole
class TestModuleTests {
	@TempDir
	Path directory;

	// Ensure no JAR files are being cached to avoid file deletion issues on Windows
	private static boolean jarDefaultUseCaches;

	@BeforeAll
	public static void setup() {
		jarDefaultUseCaches = URLConnection.getDefaultUseCaches( "jar" );
		URLConnection.setDefaultUseCaches( "jar", false );
	}

	@AfterAll
	public static void cleanup() {
		URLConnection.setDefaultUseCaches( "jar", jarDefaultUseCaches );
	}

	@Test
	void loadsFixturesIntoIndependentNamedModules() throws Exception {
		final var archive = archive();
		final var declaration = """
				/// @author Steve Ebersole
				open module fixture.module {
					exports org.hibernate.testing.orm.module.fixture;
				}
				""";
		try (var first = TestModule.load( archive, declaration, directory );
			var second = TestModule.load( archive, declaration, directory )) {
			final var fixture = first.loadClass( ModuleFixture.class.getName() );
			assertThat( first.module().isNamed() ).isTrue();
			assertThat( first.module().getName() ).isEqualTo( "fixture.module" );
			assertThat( first.module().getLayer() ).isNotSameAs( ModuleLayer.boot() );
			assertThat( fixture.getModule() ).isSameAs( first.module() );
			assertThat( fixture.getClassLoader() ).isSameAs( first.classLoader() );
			assertThat( fixture ).isNotSameAs( ModuleFixture.class );
			assertThat( second.loadClass( ModuleFixture.class.getName() ) ).isNotSameAs( fixture );
			assertThat( archive.contains( "module-info.class" ) ).isFalse();
			assertThatThrownBy( () -> first.loadClass( TestModuleTests.class.getName() ) )
					.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "does not belong" );
		}
	}

	@Test
	void reportsInvalidDeclaration() {
		assertThatThrownBy( () -> TestModule.load( archive(), "module broken {", directory ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "module-info.java" ).hasMessageContaining( "ERROR at line 1" );
	}

	@Test
	void reportsMissingAnnotationDependency() {
		assertThatThrownBy( () -> TestModule.load( archive(), """
				@missing.ModuleAnnotation
				module broken {}
				""", directory ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "module-info.java" ).hasMessageContaining( "missing" )
				.hasMessageContaining( "ERROR at line 1" );
	}

	private static JavaArchive archive() {
		return ShrinkWrap.create( JavaArchive.class, "fixture.jar" ).addClass( ModuleFixture.class );
	}
}
