/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.jar.JarOutputStream;

import javax.tools.ToolProvider;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.hibernate.orm.tooling.dialectprovider.internal.HibernateVersions;
import org.hibernate.orm.tooling.dialectprovider.internal.PluginVersions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Applies the published plugin model to an independent Gradle provider build.
///
/// @author Steve Ebersole
public class DialectProviderPluginFunctionalTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void validatesTheMainJarThroughCheckAndReusesConfigurationCache() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version );
		writeProviderBuild( version );

		final BuildResult first = runner( "check", "--configuration-cache" ).build();
		assertNotNull( first.task( ":validateDialectProviderBoundaries" ) );
		assertEquals( TaskOutcome.SUCCESS, first.task( ":validateDialectProviderBoundaries" ).getOutcome() );
		assertTrue( Files.readString( temporaryDirectory.resolve(
				"build/reports/hibernate-dialect-provider/boundary-validation.txt"
		) ).contains( "Findings: 0" ) );

		final BuildResult second = runner( "check", "--configuration-cache" ).build();
		assertTrue( second.getOutput().contains( "Reusing configuration cache" ) );
	}

	@Test
	void runsOrderedGeneratedProfilesWithProviderAuthoredTests() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version );
		seedTestKitModule( version );
		writeProviderBuild( version, true );

		final BuildResult result = runner( "dialectProviderTest", "--configuration-cache" ).build();

		assertEquals( TaskOutcome.SUCCESS, result.task( ":dialectProviderTest" ).getOutcome() );
		final String generated = Files.readString( temporaryDirectory.resolve(
				"build/generated/sources/dialectProviderTest/java/"
						+ "org/hibernate/dialect/testing/generated/GeneratedDialectProviderContractTest.java"
		) );
		assertTrue( generated.indexOf( "FirstProfile" ) < generated.indexOf( "SecondProfile" ) );
		final BuildResult reused = runner( "dialectProviderTest", "--configuration-cache" ).build();
		assertTrue( reused.getOutput().contains( "Reusing configuration cache" ) );
	}

	@Test
	void runsOneConfiguredProfile() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version );
		seedTestKitModule( version );
		writeProviderBuild( version );
		writeProviderTestSources();
		appendBuild(
				"hibernateDialectProvider { contractProfiles.add('org.example.dialect.FirstProfile') }\n"
						+ "dependencies { dialectProviderTestImplementation 'org.junit.jupiter:junit-jupiter-api:"
						+ PluginVersions.junitJupiter() + "' }\n"
		);

		final BuildResult result = runner( "dialectProviderTest" ).build();

		assertEquals( TaskOutcome.SUCCESS, result.task( ":dialectProviderTest" ).getOutcome() );
		final String generated = Files.readString( temporaryDirectory.resolve(
				"build/generated/sources/dialectProviderTest/java/"
						+ "org/hibernate/dialect/testing/generated/GeneratedDialectProviderContractTest.java"
		) );
		assertTrue( generated.contains( "FirstProfile" ) );
		assertFalse( generated.contains( "SecondProfile" ) );
	}

	@Test
	void retainsBothReportsWhenInternalAndImplementationBoundariesFail() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version, true );
		writeInvalidProviderBuild( version );

		final BuildResult result = runner( "validateDialectProviderBoundaries" ).buildAndFail();

		assertTrue( result.getOutput().contains( "found 1 error(s) and 1 warning(s)" ) );
		final Path textReport = temporaryDirectory.resolve(
				"build/reports/hibernate-dialect-provider/boundary-validation.txt"
		);
		final Path jsonReport = temporaryDirectory.resolve(
				"build/reports/hibernate-dialect-provider/boundary-validation.json"
		);
		assertTrue( Files.readString( textReport ).contains( "WARNING [INTERNAL_TARGET]" ) );
		assertTrue( Files.readString( textReport ).contains( "ERROR [MISSING_IMPLEMENT_ROLE]" ) );
		assertTrue( Files.readString( jsonReport ).contains( "\"cause\": \"INTERNAL_TARGET\"" ) );
		assertTrue( Files.readString( jsonReport ).contains( "\"cause\": \"MISSING_IMPLEMENT_ROLE\"" ) );
		assertFalse( Files.readString( jsonReport ).contains( "\"rule\"" ) );
	}

	@Test
	void internalDependenciesAreWarningsAndSucceedByDefault() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version, true );
		writeInternalProviderBuild( version );

		final BuildResult result = runner( "validateDialectProviderBoundaries" ).build();

		assertEquals( TaskOutcome.SUCCESS, result.task( ":validateDialectProviderBoundaries" ).getOutcome() );
		final String json = Files.readString( temporaryDirectory.resolve(
				"build/reports/hibernate-dialect-provider/boundary-validation.json"
		) );
		assertTrue( json.contains( "\"cause\": \"INTERNAL_TARGET\"" ) );
		assertTrue( json.contains( "\"severity\": \"WARNING\"" ) );
		assertTrue( json.contains( "\"warningCount\": 1" ) );
		assertTrue( json.contains( "\"errorCount\": 0" ) );
		assertTrue( json.contains( "\"warningsAsErrors\": false" ) );
		assertTrue( json.contains( "\"failed\": false" ) );
	}

	@Test
	void warningsAsErrorsChangesOnlyTheBuildOutcome() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version, true );
		writeInternalProviderBuild( version );
		appendBuild( "hibernateDialectProvider { warningsAsErrors = true }\n" );

		final BuildResult result = runner( "validateDialectProviderBoundaries" ).buildAndFail();

		assertTrue( result.getOutput().contains( "0 error(s) and 1 warning(s) with warnings-as-errors enabled" ) );
		final String json = Files.readString( temporaryDirectory.resolve(
				"build/reports/hibernate-dialect-provider/boundary-validation.json"
		) );
		assertTrue( json.contains( "\"severity\": \"WARNING\"" ) );
		assertFalse( json.contains( "\"severity\": \"ERROR\"" ) );
		assertTrue( json.contains( "\"warningsAsErrors\": true" ) );
		assertTrue( json.contains( "\"failed\": true" ) );
	}

	@Test
	void attachToCheckMayBeDisabledWithoutDisablingProviderTasks() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version );
		writeProviderBuild( version );
		appendBuild( "hibernateDialectProvider { attachToCheck = false }\n" );

		final BuildResult check = runner( "check" ).build();
		assertEquals( null, check.task( ":validateDialectProviderBoundaries" ) );
		final BuildResult explicit = runner( "validateDialectProviderBoundaries" ).build();
		assertEquals( TaskOutcome.SUCCESS, explicit.task( ":validateDialectProviderBoundaries" ).getOutcome() );
	}

	@Test
	void explicitVersionMustAgreeWithResolvedCore() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version );
		writeProviderBuild( version );
		appendBuild( "hibernateDialectProvider { hibernateVersion = '8.0.99' }\n" );

		final BuildResult result = runner( "validateDialectProviderBoundaries" ).buildAndFail();
		assertTrue( result.getOutput().contains( "does not agree with resolved hibernate-core" ) );
	}

	@Test
	void contractTestKitMustExactlyMatchResolvedCore() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		final String conflictingVersion = HibernateVersions.family( version ) + ".999";
		seedCoreModule( version );
		seedTestKitModule( conflictingVersion );
		writeProviderBuild( version, true );
		appendBuild(
				"dependencies {"
						+ " dialectProviderTestImplementation 'org.hibernate.orm:hibernate-dialect-testkit:"
						+ conflictingVersion + "';"
						+ " dialectProviderTestImplementation 'org.junit.jupiter:junit-jupiter-api:"
						+ PluginVersions.junitJupiter() + "' }\n"
		);

		final BuildResult result = runner( "dialectProviderTest" ).buildAndFail();
		assertTrue( result.getOutput().contains( "require org.hibernate.orm:hibernate-dialect-testkit:" + version ) );
		assertTrue( result.getOutput().contains( conflictingVersion ) );
	}

	@Test
	void supportsCustomProviderArtifactsAndMultipleProviderPackages() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version );
		writeProviderBuild( version );
		final Path customProvider = temporaryDirectory.resolve( "custom-provider.jar" );
		try ( JarOutputStream output = new JarOutputStream( Files.newOutputStream( customProvider ) ) ) {
			writeEntry( output, "org/example/first/First.class", emptyClass( "org/example/first/First" ) );
			writeEntry( output, "org/example/second/Second.class", emptyClass( "org/example/second/Second" ) );
		}
		appendBuild(
				"hibernateDialectProvider { providerPackages.set(['org.example.first', 'org.example.second']);"
						+ " providerArtifacts.setFrom(layout.projectDirectory.file('custom-provider.jar')) }\n"
		);

		final BuildResult result = runner( "validateDialectProviderBoundaries" ).build();

		assertEquals( TaskOutcome.SUCCESS, result.task( ":validateDialectProviderBoundaries" ).getOutcome() );
		assertTrue( Files.readString( temporaryDirectory.resolve(
				"build/reports/hibernate-dialect-provider/boundary-validation.txt"
		) ).contains( "Findings: 0" ) );
	}

	@Test
	void reportsMissingAndConflictingCoreComponents() throws Exception {
		writeProviderBuildWithoutCore();
		final BuildResult missing = runner( "validateDialectProviderBoundaries" ).buildAndFail();
		assertTrue( missing.getOutput().contains(
				"No org.hibernate.orm:hibernate-core component was found on the provider's main classpath"
		) );

		final String version = PluginVersions.hibernateOrm();
		final String otherVersion = HibernateVersions.family( version ) + ".999";
		seedCoreModule( version );
		seedCoreModule( otherVersion );
		writeProviderBuild( version );
		appendBuild( "dependencies { runtimeOnly 'org.hibernate.orm:hibernate-core:" + otherVersion + "' }\n" );

		final BuildResult conflicting = runner( "validateDialectProviderBoundaries" ).buildAndFail();
		assertTrue( conflicting.getOutput().contains( "Conflicting Hibernate ORM Core versions were resolved" ) );
		assertTrue( conflicting.getOutput().contains( version ) );
		assertTrue( conflicting.getOutput().contains( otherVersion ) );
	}

	@Test
	void invalidProfilesIdentifyTheClassAndCorrectiveRequirement() throws Exception {
		final String version = PluginVersions.hibernateOrm();
		seedCoreModule( version );
		seedTestKitModule( version );
		writeProviderBuild( version );
		appendBuild(
				"hibernateDialectProvider { contractProfiles.add(providers.gradleProperty('testProfile').get()) }\n"
		);
		writeInvalidProfileSources();

		assertInvalidProfile( "org.example.dialect.MissingProfile", "was not found on the dialectProviderTest runtime classpath" );
		assertInvalidProfile( "org.example.dialect.WrongType", "must implement DialectContractProfile" );
		assertInvalidProfile( "org.example.dialect.AbstractProfile", "must be concrete" );
		assertInvalidProfile( "org.example.dialect.HiddenProfile", "must be public" );
		assertInvalidProfile( "org.example.dialect.PrivateConstructorProfile",
				"must declare an accessible public no-argument constructor" );
		assertInvalidProfile( "org.example.dialect.ThrowingProfile", "constructor failed" );
	}

	private GradleRunner runner(String... arguments) {
		return GradleRunner.create()
				.withProjectDir( temporaryDirectory.toFile() )
				.withPluginClasspath()
				.withArguments( arguments )
				.forwardOutput();
	}

	private void assertInvalidProfile(String className, String requirement) throws Exception {
		final BuildResult result = runner( "dialectProviderTest", "-PtestProfile=" + className ).buildAndFail();
		final StringBuilder evidence = new StringBuilder( result.getOutput() );
		final Path testResults = temporaryDirectory.resolve( "build/test-results/dialectProviderTest" );
		if ( Files.isDirectory( testResults ) ) {
			try ( var paths = Files.walk( testResults ) ) {
				for ( Path resultFile : paths.filter( path -> path.toString().endsWith( ".xml" ) ).toList() ) {
					evidence.append( Files.readString( resultFile ) );
				}
			}
		}
		assertTrue( evidence.toString().contains( "Configured Dialect contract profile " + className ) );
		assertTrue( evidence.toString().contains( requirement ) );
	}

	private void writeProviderBuild(String version) throws Exception {
		writeProviderBuild( version, false );
	}

	private void writeProviderBuild(String version, boolean profiles) throws Exception {
		Files.writeString( temporaryDirectory.resolve( "settings.gradle" ), "rootProject.name = 'external-provider'\n" );
		Files.writeString(
				temporaryDirectory.resolve( "build.gradle" ),
				"""
				plugins {
					id 'java-library'
					id 'org.hibernate.orm.dialect-provider'
				}
				repositories {
					maven { url = uri('%s') }
					mavenCentral()
				}
				dependencies {
					compileOnly 'org.hibernate.orm:hibernate-core:%s'
				}
				hibernateDialectProvider {
					providerPackages.add('org.example.dialect')
					%s
					classificationMetadataFile = layout.projectDirectory.file('classifications.json')
				}
				""".formatted(
						temporaryDirectory.resolve( "repository" ).toUri(),
						version,
						profiles
								? "contractProfiles.addAll(['org.example.dialect.FirstProfile', 'org.example.dialect.SecondProfile'])"
								: ""
				)
		);
		final Path source = temporaryDirectory.resolve( "src/main/java/org/example/dialect/ExampleDialect.java" );
		Files.createDirectories( source.getParent() );
		Files.writeString(
				source,
				"package org.example.dialect; public final class ExampleDialect {}\n"
		);
		Files.writeString(
				temporaryDirectory.resolve( "classifications.json" ),
				"{\"schema\":\"hibernate-orm-classifications\",\"schemaVersion\":1,"
						+ "\"hibernateVersion\":\"" + HibernateVersions.family( version ) + "\","
						+ "\"sourceVersion\":\"" + version + "\",\"elements\":[]}"
		);
		if ( profiles ) {
			writeProviderTestSources();
		}
	}

	private void writeProviderBuildWithoutCore() throws Exception {
		Files.writeString( temporaryDirectory.resolve( "settings.gradle" ), "rootProject.name = 'external-provider'\n" );
		Files.writeString(
				temporaryDirectory.resolve( "build.gradle" ),
				"plugins { id 'java-library'; id 'org.hibernate.orm.dialect-provider' }\n"
						+ "hibernateDialectProvider { providerPackages.add('org.example.dialect') }\n"
		);
		final Path source = temporaryDirectory.resolve( "src/main/java/org/example/dialect/ExampleDialect.java" );
		Files.createDirectories( source.getParent() );
		Files.writeString( source, "package org.example.dialect; public final class ExampleDialect {}\n" );
	}

	private void writeInvalidProfileSources() throws Exception {
		final Path packageDirectory = temporaryDirectory.resolve( "src/dialectProviderTest/java/org/example/dialect" );
		Files.createDirectories( packageDirectory );
		Files.writeString(
				packageDirectory.resolve( "WrongType.java" ),
				"package org.example.dialect; public final class WrongType {}\n"
		);
		Files.writeString(
				packageDirectory.resolve( "AbstractProfile.java" ),
				"package org.example.dialect; public abstract class AbstractProfile"
						+ " implements org.hibernate.dialect.testing.spi.DialectContractProfile {}\n"
		);
		Files.writeString(
				packageDirectory.resolve( "HiddenProfile.java" ),
				"package org.example.dialect; final class HiddenProfile"
						+ " implements org.hibernate.dialect.testing.spi.DialectContractProfile {}\n"
		);
		Files.writeString(
				packageDirectory.resolve( "PrivateConstructorProfile.java" ),
				"package org.example.dialect; public final class PrivateConstructorProfile"
						+ " implements org.hibernate.dialect.testing.spi.DialectContractProfile {"
						+ " private PrivateConstructorProfile() {} }\n"
		);
		Files.writeString(
				packageDirectory.resolve( "ThrowingProfile.java" ),
				"package org.example.dialect; public final class ThrowingProfile"
						+ " implements org.hibernate.dialect.testing.spi.DialectContractProfile {"
						+ " public ThrowingProfile() { throw new IllegalStateException(\"boom\"); } }\n"
		);
	}

	private void writeProviderTestSources() throws Exception {
		final Path packageDirectory = temporaryDirectory.resolve( "src/dialectProviderTest/java/org/example/dialect" );
		Files.createDirectories( packageDirectory );
		for ( String name : new String[] { "FirstProfile", "SecondProfile" } ) {
			Files.writeString(
					packageDirectory.resolve( name + ".java" ),
					"package org.example.dialect; public final class " + name
							+ " implements org.hibernate.dialect.testing.spi.DialectContractProfile {}\n"
			);
		}
		Files.writeString(
				packageDirectory.resolve( "ProviderAuthoredTest.java" ),
				"package org.example.dialect; public class ProviderAuthoredTest {"
						+ " @org.junit.jupiter.api.Test void providerAssertion() {} }\n"
		);
	}

	private void appendBuild(String value) throws Exception {
		Files.writeString( temporaryDirectory.resolve( "build.gradle" ), value, StandardOpenOption.APPEND );
	}

	private void writeInvalidProviderBuild(String version) throws Exception {
		writeProviderBuild( version );
		final Path source = temporaryDirectory.resolve( "src/main/java/org/example/dialect/ExampleDialect.java" );
		Files.writeString(
				source,
				"package org.example.dialect;"
						+ " public final class ExampleDialect implements org.hibernate.spi.UseOnly {"
						+ " public void call() { org.hibernate.internal.Hidden.touch(); } }\n"
		);
		Files.writeString(
				temporaryDirectory.resolve( "classifications.json" ),
				"{\"schema\":\"hibernate-orm-classifications\",\"schemaVersion\":1,"
						+ "\"hibernateVersion\":\"" + HibernateVersions.family( version ) + "\","
						+ "\"sourceVersion\":\"" + version + "\",\"elements\":["
						+ element( "type:org.hibernate.spi.UseOnly", "SPI", "USE" ) + ","
						+ element( "type:org.hibernate.internal.Hidden", "INTERNAL", "" ) + ","
						+ element( "method:org.hibernate.internal.Hidden#touch()", "INTERNAL", "" )
						+ "]}"
		);
	}

	private void writeInternalProviderBuild(String version) throws Exception {
		writeProviderBuild( version );
		final Path source = temporaryDirectory.resolve( "src/main/java/org/example/dialect/ExampleDialect.java" );
		Files.writeString(
				source,
				"package org.example.dialect; public final class ExampleDialect {"
						+ " public void call() { org.hibernate.internal.Hidden.touch(); } }\n"
		);
		Files.writeString(
				temporaryDirectory.resolve( "classifications.json" ),
				"{\"schema\":\"hibernate-orm-classifications\",\"schemaVersion\":1,"
						+ "\"hibernateVersion\":\"" + HibernateVersions.family( version ) + "\","
						+ "\"sourceVersion\":\"" + version + "\",\"elements\":["
						+ element( "type:org.hibernate.internal.Hidden", "INTERNAL", "" ) + ","
						+ element( "method:org.hibernate.internal.Hidden#touch()", "INTERNAL", "" )
						+ "]}"
		);
	}

	private static String element(String id, String category, String role) {
		return "{\"id\":\"" + id + "\",\"category\":\"" + category + "\",\"spiRoles\":"
				+ ( role.isEmpty() ? "[]" : "[\"" + role + "\"]" )
				+ ",\"artifact\":\"hibernate-core\"}";
	}

	private void seedCoreModule(String version) throws IOException {
		seedCoreModule( version, false );
	}

	private void seedCoreModule(String version, boolean boundaryContracts) throws IOException {
		final Path module = temporaryDirectory.resolve( "repository/org/hibernate/orm/hibernate-core/" + version );
		Files.createDirectories( module );
		try ( JarOutputStream output = new JarOutputStream( Files.newOutputStream(
				module.resolve( "hibernate-core-" + version + ".jar" )
		) ) ) {
			if ( boundaryContracts ) {
				writeEntry( output, "org/hibernate/spi/UseOnly.class", interfaceClass( "org/hibernate/spi/UseOnly" ) );
				writeEntry( output, "org/hibernate/internal/Hidden.class", hiddenClass() );
			}
		}
		Files.writeString(
				module.resolve( "hibernate-core-" + version + ".pom" ),
				"""
				<project xmlns="http://maven.apache.org/POM/4.0.0">
				<modelVersion>4.0.0</modelVersion>
				<groupId>org.hibernate.orm</groupId>
				<artifactId>hibernate-core</artifactId>
				<version>%s</version>
				</project>
				""".formatted( version ),
				StandardCharsets.UTF_8
		);
	}

	private static byte[] interfaceClass(String name) {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
				name, null, "java/lang/Object", null );
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] emptyClass(String name) {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null );
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] hiddenClass() {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC, "org/hibernate/internal/Hidden", null, "java/lang/Object", null );
		final MethodVisitor method = writer.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "touch", "()V", null, null );
		method.visitCode();
		method.visitInsn( Opcodes.RETURN );
		method.visitMaxs( 0, 0 );
		method.visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static void writeEntry(JarOutputStream output, String name, byte[] contents) throws IOException {
		output.putNextEntry( new java.util.jar.JarEntry( name ) );
		output.write( contents );
		output.closeEntry();
	}

	private void seedTestKitModule(String version) throws Exception {
		final Path sources = temporaryDirectory.resolve( "testkit-sources" );
		final Path classes = temporaryDirectory.resolve( "testkit-classes" );
		final Path profile = sources.resolve( "org/hibernate/dialect/testing/spi/DialectContractProfile.java" );
		final Path kit = sources.resolve( "org/hibernate/dialect/testing/DialectTestKit.java" );
		Files.createDirectories( profile.getParent() );
		Files.createDirectories( kit.getParent() );
		Files.writeString(
				profile,
				"package org.hibernate.dialect.testing.spi; public interface DialectContractProfile {}\n"
		);
		Files.writeString(
				kit,
				"package org.hibernate.dialect.testing;"
						+ " public final class DialectTestKit {"
						+ " public static org.junit.jupiter.api.DynamicContainer contractTests("
						+ "org.hibernate.dialect.testing.spi.DialectContractProfile profile) {"
						+ " return org.junit.jupiter.api.DynamicContainer.dynamicContainer("
						+ "profile.getClass().getSimpleName(), java.util.List.of()); } }\n"
		);
		Files.createDirectories( classes );
		final String junitClasspath = org.junit.jupiter.api.DynamicContainer.class
				.getProtectionDomain().getCodeSource().getLocation().getPath();
		final int compilation = ToolProvider.getSystemJavaCompiler().run(
				null, null, null,
				"-classpath", junitClasspath,
				"-d", classes.toString(),
				profile.toString(), kit.toString()
		);
		if ( compilation != 0 ) {
			throw new IllegalStateException( "Unable to compile the seeded test-kit fixture" );
		}
		final Path module = temporaryDirectory.resolve( "repository/org/hibernate/orm/hibernate-dialect-testkit/" + version );
		Files.createDirectories( module );
		try ( JarOutputStream output = new JarOutputStream( Files.newOutputStream(
				module.resolve( "hibernate-dialect-testkit-" + version + ".jar" )
		) ) ) {
			try ( var paths = Files.walk( classes ) ) {
				for ( Path classFile : paths.filter( path -> path.toString().endsWith( ".class" ) )
						.sorted( Comparator.comparing( Path::toString ) ).toList() ) {
					final String entryName = classes.relativize( classFile ).toString().replace( java.io.File.separatorChar, '/' );
					output.putNextEntry( new java.util.jar.JarEntry( entryName ) );
					output.write( Files.readAllBytes( classFile ) );
					output.closeEntry();
				}
			}
		}
		Files.writeString(
				module.resolve( "hibernate-dialect-testkit-" + version + ".pom" ),
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion>"
						+ "<groupId>org.hibernate.orm</groupId><artifactId>hibernate-dialect-testkit</artifactId>"
						+ "<version>" + version + "</version></project>\n"
		);
	}
}
