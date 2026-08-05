/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.SPI;
import org.hibernate.orm.post.SpiModel.Role;
import org.hibernate.orm.post.fixture.validation.internal.InternalPackageContract;
import org.hibernate.orm.post.fixture.validation.spi.InternalSpiContract;
import org.hibernate.orm.post.fixture.validation.spi.ValidConventionalContract;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI001;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI002;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI003;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI004;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI005;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI006;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI007;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI008;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI009;
import static org.hibernate.orm.post.SpiValidator.Rule.SPI010;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests each SPI validation rule and the migration-allowlist contract.
///
/// @author Steve Ebersole
public class SpiValidatorTests {
	private final SpiValidator validator = new SpiValidator();

	@Test
	public void spi001RejectsConflictingClassification() throws IOException {
		assertRule( classify( Conflict.class ), SpiValidator.Evidence.NONE, SPI001, typeId( Conflict.class ) );
	}

	@Test
	public void spi002RejectsInternalPackage() throws IOException {
		assertRule(
				classify( InternalPackageContract.class ),
				SpiValidator.Evidence.NONE,
				SPI002,
				typeId( InternalPackageContract.class )
		);
	}

	@Test
	public void spi003ReportsShortestInternalSignaturePath() throws IOException {
		final SpiValidator.Diagnostic diagnostic = assertRule(
				classify( SignatureRoot.class, InternalSignatureType.class ),
				SpiValidator.Evidence.NONE,
				SPI003,
				typeId( InternalSignatureType.class )
		);
		assertTrue( diagnostic.getPath().size() > 1 );
		assertEquals( typeId( InternalSignatureType.class ), diagnostic.getPath().get( diagnostic.getPath().size() - 1 ) );
		assertEquals( roles( Role.USE ), diagnostic.getRoles() );
	}

	@Test
	public void spi004RejectsEmptyAndInvalidRoles() throws IOException {
		assertRule( classify( EmptyRoles.class ), SpiValidator.Evidence.NONE, SPI004, typeId( EmptyRoles.class ) );
		assertRule(
				classify( InvalidFieldRole.class ),
				SpiValidator.Evidence.NONE,
				SPI004,
				fieldId( InvalidFieldRole.class, "hook" )
		);
		assertRule(
				classify( InvalidConstructorRole.class ),
				SpiValidator.Evidence.NONE,
				SPI004,
				constructorId( InvalidConstructorRole.class )
		);
	}

	@Test
	public void spi005RejectsInvalidImplementationPoints() throws IOException {
		assertRule(
				classify( FinalImplementation.class ),
				SpiValidator.Evidence.NONE,
				SPI005,
				typeId( FinalImplementation.class )
		);
		assertRule(
				classify( StaticImplementationMethod.class ),
				SpiValidator.Evidence.NONE,
				SPI005,
				methodId( StaticImplementationMethod.class, "hook" )
		);
		assertRule(
				classify( MissingSubclassConstructor.class ),
				SpiValidator.Evidence.NONE,
				SPI005,
				typeId( MissingSubclassConstructor.class )
		);
	}

	@Test
	public void spi006ConsumesOverridePointEvidence() throws IOException {
		final String method = "method:external.ProviderHook#customize()";
		assertRule(
				classify(),
				evidence( SPI006, method, roles( Role.IMPLEMENT ), Arrays.asList( "source:javadoc", method ) ),
				SPI006,
				method
		);
	}

	@Test
	public void spi007RejectsInternalTypeInExactSpiPackage() throws IOException {
		assertRule(
				classify( InternalSpiContract.class ),
				SpiValidator.Evidence.NONE,
				SPI007,
				typeId( InternalSpiContract.class )
		);
	}

	@Test
	public void spi008ConsumesProviderBoundaryEvidence() throws IOException {
		final String internalType = "type:org.hibernate.internal.EngineState";
		assertRule(
				classify(),
				evidence(
						SPI008,
						internalType,
						roles( Role.USE ),
						Arrays.asList( "provider:example.Dialect", internalType )
				),
				SPI008,
				internalType
		);
	}

	@Test
	public void spi009ConsumesCompatibilityEvidence() throws IOException {
		final String element = "method:fixture.Contract#operation()";
		assertRule(
				classify(),
				evidence( SPI009, element, roles( Role.IMPLEMENT ), Collections.singletonList( element ) ),
				SPI009,
				element
		);
	}

	@Test
	public void spi010ConsumesSupplyDocumentationEvidence() throws IOException {
		final SpiModel model = classify( UndocumentedSupply.class );
		final String element = typeId( UndocumentedSupply.class );
		assertRule(
				model,
				evidence( SPI010, element, roles( Role.SUPPLY ), Collections.singletonList( element ) ),
				SPI010,
				element
		);
	}

	@Test
	public void positiveProviderShapesPass() throws IOException {
		final SpiModel model = classify(
				ValidImplementedSpi.class,
				UseOnlySpi.class,
				ImmutableSupply.class,
				SubclassableSpi.class,
				InternalImplementation.class,
				ValidConventionalContract.class
		);
		final SpiValidator.Result result = validator.validate(
				model,
				SpiValidator.Evidence.NONE,
				SpiValidationAllowlist.empty()
		);
		assertFalse( result.hasFailures(), new SpiValidationReportRenderer().render( result ) );
		assertTrue( result.getDiagnostics().isEmpty(), new SpiValidationReportRenderer().render( result ) );

		assertEquals( roles( Role.IMPLEMENT ), required( model, typeId( ValidImplementedSpi.class ) ).getEffectiveRoles() );
		assertEquals( roles( Role.SUPPLY ), required( model, typeId( ImmutableSupply.class ) ).getEffectiveRoles() );
		assertFalse(
				required( model, methodId( SubclassableSpi.class, "finalVocabulary" ) )
						.getEffectiveRoles()
						.contains( Role.USE )
		);
		assertNull( model.getElement( typeId( InternalImplementation.class ) ) );
		assertEquals(
				roles( Role.USE ),
				required( model, typeId( ValidConventionalContract.class ) ).getEffectiveRoles()
		);
	}

	@Test
	public void allowlistedHistoricalErrorDoesNotHideNewError(@TempDir Path temporaryDirectory) throws IOException {
		final SpiModel model = classify( Conflict.class, FinalImplementation.class );
		final SpiValidationAllowlist allowlist = readAllowlist(
				temporaryDirectory,
				entry( SPI001, typeId( Conflict.class ) )
		);
		final SpiValidator.Result result = validator.validate( model, SpiValidator.Evidence.NONE, allowlist );
		final SpiValidator.Diagnostic historical = diagnostic( result, SPI001, typeId( Conflict.class ) );
		assertNotNull( historical.getAllowlistMatch() );
		assertNotNull( diagnostic( result, SPI005, typeId( FinalImplementation.class ) ) );
		assertTrue( result.hasFailures(), "The unrelated new error must still fail validation" );
	}

	@Test
	public void unusedAllowlistEntryFails(@TempDir Path temporaryDirectory) throws IOException {
		final SpiValidationAllowlist allowlist = readAllowlist(
				temporaryDirectory,
				entry( SPI001, "type:fixture.RemovedViolation" )
		);
		final SpiValidator.Result result = validator.validate( classify(), SpiValidator.Evidence.NONE, allowlist );
		assertTrue( result.hasFailures() );
		assertEquals( 1, result.getConfigurationErrors().size() );
	}

	@Test
	public void malformedAllowlistIsRejected(@TempDir Path temporaryDirectory) throws IOException {
		final Path allowlist = temporaryDirectory.resolve( "allowlist.json" );
		Files.writeString(
				allowlist,
				"{\"schema\":\"hibernate-orm-spi-validation-allowlist\",\"schemaVersion\":1,"
						+ "\"entries\":[{\"rule\":\"SPI001\",\"element\":\"type:fixture.Bad\"}]}",
				StandardCharsets.UTF_8
		);
		assertThrows( IllegalArgumentException.class, () -> SpiValidationAllowlist.read( allowlist.toFile() ) );
	}

	@Test
	public void diagnosticsContainRequiredContext() throws IOException {
		final SpiValidator.Result result = validator.validate(
				classify( SignatureRoot.class, InternalSignatureType.class ),
				SpiValidator.Evidence.NONE,
				SpiValidationAllowlist.empty()
		);
		final String report = new SpiValidationReportRenderer().render( result );
		assertTrue( report.contains( "[ERROR] SPI003" ) );
		assertTrue( report.contains( "Roles: USE" ) );
		assertTrue( report.contains( "Origins:" ) );
		assertTrue( report.contains( "Path:" ) );
		assertTrue( report.contains( "Allowlist: none" ) );
		assertTrue( report.contains( "Remediation:" ) );
	}

	@Test
	public void validationTaskIsRegisteredButNotYetInCheckLifecycle() {
		final Project project = ProjectBuilder.builder().build();
		new ReportGenerationPlugin().apply( project );
		final Task validation = project.getTasks().getByName( "validateSpi" );
		assertNotNull( validation );
		assertEquals( SpiValidationTask.class, validation.getClass().getSuperclass() );
		assertDoesNotThrow( () -> project.getTasks().getByName( "generateReports" ) );
	}

	private SpiValidator.Diagnostic assertRule(
			SpiModel model,
			SpiValidator.Evidence evidence,
			SpiValidator.Rule rule,
			String elementId) {
		final SpiValidator.Result result = validator.validate( model, evidence, SpiValidationAllowlist.empty() );
		return diagnostic( result, rule, elementId );
	}

	private static SpiValidator.Diagnostic diagnostic(
			SpiValidator.Result result,
			SpiValidator.Rule rule,
			String elementId) {
		for ( SpiValidator.Diagnostic diagnostic : result.getDiagnostics() ) {
			if ( diagnostic.getRule() == rule && diagnostic.getElementId().equals( elementId ) ) {
				return diagnostic;
			}
		}
		throw new AssertionError( "Missing " + rule + " for " + elementId + ": "
				+ new SpiValidationReportRenderer().render( result ) );
	}

	private static SpiValidator.Evidence evidence(
			SpiValidator.Rule rule,
			String element,
			Set<Role> roles,
			List<String> path) {
		return SpiValidator.Evidence.builder()
				.add( rule, element, roles, path, "Negative fixture for " + rule.getId() )
				.build();
	}

	private static SpiModel classify(Class<?>... fixtures) throws IOException {
		final Indexer indexer = new Indexer();
		indexClass( indexer, SPI.class );
		indexClass( indexer, Internal.class );
		indexClass( indexer, Incubating.class );
		for ( Class<?> fixture : fixtures ) {
			indexClass( indexer, fixture );
		}
		final Index index = indexer.complete();
		return new SpiJandexClassifier().classify( index );
	}

	private static void indexClass(Indexer indexer, Class<?> type) throws IOException {
		final String resourceName = "/" + type.getName().replace( '.', '/' ) + ".class";
		try ( InputStream stream = SpiValidatorTests.class.getResourceAsStream( resourceName ) ) {
			assertNotNull( stream, resourceName );
			indexer.index( stream );
		}
	}

	private static SpiValidationAllowlist readAllowlist(Path directory, String entries) throws IOException {
		final Path file = directory.resolve( "allowlist.json" );
		Files.writeString(
				file,
				"{\n  \"schema\": \"hibernate-orm-spi-validation-allowlist\",\n"
						+ "  \"schemaVersion\": 1,\n  \"entries\": [" + entries + "]\n}\n",
				StandardCharsets.UTF_8
		);
		return SpiValidationAllowlist.read( file.toFile() );
	}

	private static String entry(SpiValidator.Rule rule, String element) {
		return "{\"rule\":\"" + rule.getId() + "\",\"element\":\"" + element
				+ "\",\"owner\":\"HHH-00001\",\"reason\":\"migration\",\"removalRelease\":\"8.2\"}";
	}

	private static SpiModel.Element required(SpiModel model, String id) {
		final SpiModel.Element element = model.getElement( id );
		assertNotNull( element, id );
		return element;
	}

	private static String typeId(Class<?> type) {
		return SpiJandexClassifier.typeId( type.getName() );
	}

	private static String fieldId(Class<?> type, String field) {
		return "field:" + type.getName() + '#' + field;
	}

	private static String methodId(Class<?> type, String method) {
		return "method:" + type.getName() + '#' + method + "()";
	}

	private static String constructorId(Class<?> type) {
		return "constructor:" + type.getName() + "#<init>()";
	}

	private static Set<Role> roles(Role... roles) {
		return roles.length == 0 ? EnumSet.noneOf( Role.class ) : EnumSet.copyOf( Arrays.asList( roles ) );
	}

	@Internal
	@SPI
	public interface Conflict {
	}

	@SPI
	public interface SignatureRoot {
		InternalSignatureType state();
	}

	@Internal
	public interface InternalSignatureType {
	}

	@SPI({})
	public interface EmptyRoles {
	}

	public static class InvalidFieldRole {
		@SPI(IMPLEMENT)
		public Object hook;
	}

	public static class InvalidConstructorRole {
		@SPI(SUPPLY)
		public InvalidConstructorRole() {
		}
	}

	@SPI(IMPLEMENT)
	public static final class FinalImplementation {
		@SPI(IMPLEMENT)
		public FinalImplementation() {
		}
	}

	@SPI(IMPLEMENT)
	public static class StaticImplementationMethod {
		@SPI(IMPLEMENT)
		protected StaticImplementationMethod() {
		}

		@SPI(IMPLEMENT)
		public static void hook() {
		}
	}

	@SPI(IMPLEMENT)
	public static class MissingSubclassConstructor {
		private MissingSubclassConstructor() {
		}
	}

	@SPI(SUPPLY)
	public static final class UndocumentedSupply {
	}

	@Incubating
	@SPI(IMPLEMENT)
	public interface ValidImplementedSpi {
		void execute();
	}

	@SPI(USE)
	public interface UseOnlySpi {
		String value();
	}

	@SPI(SUPPLY)
	public static final class ImmutableSupply {
		public String value() {
			return "value";
		}
	}

	@SPI(IMPLEMENT)
	public static class SubclassableSpi {
		@SPI(IMPLEMENT)
		protected SubclassableSpi() {
		}

		public void overridePoint() {
		}

		public final void finalVocabulary() {
		}
	}

	@Internal
	public static class InternalImplementation implements ValidImplementedSpi {
		@Override
		public void execute() {
		}
	}
}
