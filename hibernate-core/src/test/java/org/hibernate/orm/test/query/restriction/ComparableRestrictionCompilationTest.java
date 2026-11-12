package org.hibernate.orm.test.query.restriction;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.tools.ToolProvider;

import org.hibernate.query.restriction.Restriction;
import org.hibernate.testing.orm.junit.Jira;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.annotation.Nonnull;
import jakarta.persistence.metamodel.SingularAttribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jira("https://hibernate.atlassian.net/browse/HHH-20939")
class ComparableRestrictionCompilationTest {

	@TempDir
	Path temporaryDirectory;

	@ParameterizedTest
	@ValueSource(strings = { "java.time.LocalDate", "java.time.LocalDateTime", "String" })
	void testComparableValues(String type) throws Exception {
		assertCompiles( """
				import java.util.List;
				import jakarta.persistence.metamodel.SingularAttribute;
				import org.hibernate.query.range.Range;
				import org.hibernate.query.restriction.Restriction;
				class TestClient {
					static List<Restriction<TestClient>> restrictions(
							SingularAttribute<TestClient, %1$s> attribute, %1$s lower, %1$s upper) {
						return List.of(
							Restriction.greaterThan(attribute, lower),
							Restriction.greaterThanOrEqual(attribute, lower),
							Restriction.lessThan(attribute, upper),
							Restriction.lessThanOrEqual(attribute, upper),
							Restriction.between(attribute, lower, upper),
							Restriction.notBetween(attribute, lower, upper));
					}
					static List<Range<%1$s>> ranges(%1$s lower, %1$s upper) {
						return List.of(
							Range.greaterThan(lower),
							Range.greaterThanOrEqualTo(lower),
							Range.lessThan(upper),
							Range.lessThanOrEqualTo(upper),
							Range.open(lower, upper),
							Range.closed(lower, upper));
					}
				}
				""".formatted( type ), true );
	}

	@Test
	void testExplicitComparableSupertype() throws Exception {
		assertCompiles( """
				import java.time.LocalDate;
				import java.time.chrono.ChronoLocalDate;
				import org.hibernate.query.range.Range;
				class TestClient {
					static void ranges(LocalDate date) {
						Range<ChronoLocalDate> explicitTarget = Range.greaterThan(date);
						var explicitWitness = Range.<ChronoLocalDate>greaterThan(date);
						Range<ChronoLocalDate> sameType = explicitWitness;
					}
				}
				""", true );
	}

	@Test
	void testNonComparableRestriction() throws Exception {
		assertCompiles( """
				import jakarta.persistence.metamodel.SingularAttribute;
				import org.hibernate.query.restriction.Restriction;
				class TestClient {
					static Restriction<TestClient> restriction(SingularAttribute<TestClient, Object> attribute) {
						return Restriction.greaterThan(attribute, new Object());
					}
				}
				""", false );
	}

	@Test
	void testNonComparableRange() throws Exception {
		assertCompiles( """
				import org.hibernate.query.range.Range;
				class TestClient {
					static Range<Object> range() {
						return Range.greaterThan(new Object());
					}
				}
				""", false );
	}

	private void assertCompiles(String source, boolean expected) throws Exception {
		final var compiler = ToolProvider.getSystemJavaCompiler();
		assertNotNull( compiler, "A JDK compiler is required" );
		final var sourceFile = temporaryDirectory.resolve( "TestClient.java" );
		Files.writeString( sourceFile, source );
		final var output = Files.createDirectory( temporaryDirectory.resolve( "classes" ) );
		final var classPath = String.join( File.pathSeparator,
				classLocation( Restriction.class ), classLocation( SingularAttribute.class ),
				classLocation( Nonnull.class ) );
		final var diagnostics = new StringWriter();
		try (var files = compiler.getStandardFileManager( null, Locale.ROOT, null )) {
			final var options = List.of( "-proc:none", "--release", "17", "-Xlint:unchecked", "-Werror",
					"-classpath", classPath, "-d", output.toString() );
			final var units = files.getJavaFileObjectsFromPaths( List.of( sourceFile ) );
			final var task = compiler.getTask( diagnostics, files, null, options, null, units );
			task.setLocale( Locale.ROOT );
			final var compiled = task.call();
			assertEquals( expected, compiled, () -> "Unexpected client compilation result: " + diagnostics );
			if ( !expected ) {
				assertTrue( diagnostics.toString().contains( "Comparable" ), diagnostics::toString );
			}
		}
	}

	private static String classLocation(Class<?> type) throws Exception {
		return Path.of( type.getProtectionDomain().getCodeSource().getLocation().toURI() ).toString();
	}
}
