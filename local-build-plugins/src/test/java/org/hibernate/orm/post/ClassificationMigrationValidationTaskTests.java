package org.hibernate.orm.post;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;

/// Gradle input, execution, and deterministic-report coverage for migration
/// validation.
///
/// @author Steve Ebersole
public class ClassificationMigrationValidationTaskTests {
	@TempDir
	Path temporaryDirectory;

	@Test
	public void prereleaseFindingsWarnWhileStrictBaselinesFail() throws Exception {
		Files.writeString( temporaryDirectory.resolve( "settings.gradle" ), "" );
		Files.writeString( temporaryDirectory.resolve( "build.gradle" ),
				"plugins { id 'org.hibernate.orm.build.reports' }\n"
						+ "tasks.register('validateFixture', org.hibernate.orm.post.ClassificationMigrationValidationTask) {\n"
						+ "    baselineClassificationMetadataFile = layout.projectDirectory.file('baseline.json')\n"
						+ "    currentClassificationMetadataFile = layout.projectDirectory.file('current.json')\n"
						+ "    baselineArtifacts.from('baseline-classes')\n"
						+ "    currentArtifacts.from('current-classes')\n"
						+ "}\n" );
		writeContract( temporaryDirectory.resolve( "baseline-classes" ) );
		Files.createDirectories( temporaryDirectory.resolve( "current-classes" ) );
		final ClassificationMetadataJson json = new ClassificationMetadataJson();
		Files.writeString( temporaryDirectory.resolve( "current.json" ),
				json.write( new ClassificationMetadata( "8.1", "8.1.0-SNAPSHOT", ClassificationModel.builder().build() ) ) );
		final GradleRunner runner = GradleRunner.create().withProjectDir( temporaryDirectory.toFile() )
				.withPluginClasspath().withArguments( "validateFixture", "--offline" );
		for ( String version : new String[] { "8.0.0.Alpha1", "8.0.0.Beta3", "8.0.0.CR1", "8.0.0.Final" } ) {
			Files.writeString( temporaryDirectory.resolve( "baseline.json" ),
					json.write( new ClassificationMetadata( "8.0", version, model() ) ) );
			final boolean advisory = version.contains( "Alpha" ) || version.contains( "Beta" ) || version.contains( "CR" );
			final var result = advisory ? runner.build() : runner.buildAndFail();
			assertEquals( advisory ? SUCCESS : FAILED, result.task( ":validateFixture" ).getOutcome() );
			final Path reportPath = temporaryDirectory.resolve( "build/orm/reports/migration-compatibility-validation.txt" );
			final String report = Files.readString( reportPath );
			assertTrue( report.contains( "[ERROR] API_COMPATIBILITY_REGRESSION" ) );
			if ( advisory ) {
				assertTrue( report.startsWith( "Hibernate ORM migration compatibility: PASSED_WITH_WARNINGS\n" ) );
				assertTrue( result.getOutput().contains( "Migration compatibility findings against baseline " + version ) );
				assertTrue( result.getOutput().contains( "nonblocking because the baseline is an Alpha/Beta/CR release (ERROR=1, REVIEW=0); see " ), result.getOutput() );
				assertTrue( result.getOutput().contains( reportPath.toRealPath().toString() ), result.getOutput() );
			}
			else {
				assertTrue( report.startsWith( "Hibernate ORM migration compatibility: FAILED\n" ) );
				assertFalse( result.getOutput().contains( "Migration compatibility findings against baseline" ) );
			}
		}

		Files.writeString( temporaryDirectory.resolve( "baseline.json" ),
				json.write( new ClassificationMetadata( "8.0", "8.0.0.CR1", model() ) ) );
		writeContract( temporaryDirectory.resolve( "current-classes" ) );
		Files.writeString( temporaryDirectory.resolve( "current.json" ),
				json.write( new ClassificationMetadata( "8.1", "8.1.0-SNAPSHOT", model() ) ) );
		final var compatible = runner.build();
		assertFalse( compatible.getOutput().contains( "Migration compatibility findings against baseline" ) );
		assertTrue( Files.readString( temporaryDirectory.resolve( "build/orm/reports/migration-compatibility-validation.txt" ) )
				.startsWith( "Hibernate ORM migration compatibility: PASSED\n" ) );

		Files.writeString( temporaryDirectory.resolve( "current.json" ), "{}" );
		final var invalid = runner.buildAndFail();
		assertEquals( FAILED, invalid.task( ":validateFixture" ).getOutcome() );
		assertTrue( Files.readString( temporaryDirectory.resolve( "build/orm/reports/migration-compatibility-validation.txt" ) )
				.contains( "Configuration error:" ) );
		assertFalse( invalid.getOutput().contains( "Migration compatibility findings against baseline" ) );
	}

	@Test
	public void executesWithExplicitBaselineAndCurrentInputs() throws Exception {
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		project.getLayout().getBuildDirectory().set( temporaryDirectory.resolve( "target" ).toFile() );
		new ReportGenerationPlugin().apply( project );

		final Path baselineClasses = temporaryDirectory.resolve( "baseline-classes" );
		final Path currentClasses = temporaryDirectory.resolve( "current-classes" );
		writeContract( baselineClasses );
		writeContract( currentClasses );
		final Path baselineMetadata = temporaryDirectory.resolve( "baseline.json.gz" );
		final Path currentMetadata = temporaryDirectory.resolve( "current.json" );
		final ClassificationMetadataJson json = new ClassificationMetadataJson();
		final String baselineDocument = json.write( new ClassificationMetadata( "8.1", "8.1.0", model() ) );
		Files.write( baselineMetadata, json.gzip( baselineDocument ) );
		Files.writeString(
				currentMetadata,
				json.write( new ClassificationMetadata( "8.1", "8.1.1", model() ) ),
				StandardCharsets.UTF_8
		);

		final ClassificationMigrationValidationTask task = (ClassificationMigrationValidationTask) project.getTasks()
				.getByName( "validateMigrationCompatibility" );
		task.getBaselineClassificationMetadataFile().set( baselineMetadata.toFile() );
		task.getCurrentClassificationMetadataFile().set( currentMetadata.toFile() );
		task.getBaselineArtifacts().setFrom( baselineClasses );
		task.getCurrentArtifacts().setFrom( currentClasses );
		task.validateMigration();

		final String report = Files.readString( task.getReportFile().get().getAsFile().toPath() );
		assertTrue( report.startsWith( "Hibernate ORM migration compatibility: PASSED" ) );
		assertTrue( report.contains( "Baseline source version: 8.1.0" ) );
		assertTrue( report.contains( "Current source version: 8.1.1" ) );
		assertTrue( report.contains( "Classification schema: hibernate-orm-classifications v1" ) );
	}

	@Test
	public void newMajorWithoutBaselineIsNotApplicable() throws Exception {
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		project.getLayout().getBuildDirectory().set( temporaryDirectory.resolve( "target" ).toFile() );
		new ReportGenerationPlugin().apply( project );
		final ClassificationMigrationValidationTask task = (ClassificationMigrationValidationTask) project.getTasks()
				.getByName( "validateMigrationCompatibility" );
		task.getBaselineClassificationMetadataFile().unset();
		task.validateMigration();
		assertTrue(
				Files.readString( task.getReportFile().get().getAsFile().toPath() )
						.contains( "migration compatibility: NOT_APPLICABLE" )
		);
	}

	@Test
	public void bootstrapCannotBypassEstablishedBaseline() throws Exception {
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		project.getLayout().getBuildDirectory().set( temporaryDirectory.resolve( "target" ).toFile() );
		new ReportGenerationPlugin().apply( project );
		final Path baselineMetadata = temporaryDirectory.resolve( "established-baseline.json" );
		Files.writeString(
				baselineMetadata,
				new ClassificationMetadataJson().write( new ClassificationMetadata( "8.0", "8.0.7", model() ) )
		);
		final ClassificationMigrationValidationTask task = (ClassificationMigrationValidationTask) project.getTasks()
				.getByName( "validateMigrationCompatibility" );
		task.getBaselineFamily().set( "8.0" );
		task.getBootstrapBaseline().set( true );
		task.getBaselineClassificationMetadataFile().set( baselineMetadata.toFile() );
		assertThrows( org.gradle.api.GradleException.class, task::validateMigration );
	}

	@Test
	public void generatesIndependentNonGatingReview() throws Exception {
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		project.getLayout().getBuildDirectory().set( temporaryDirectory.resolve( "target" ).toFile() );
		new ReportGenerationPlugin().apply( project );

		final Path baselineClasses = temporaryDirectory.resolve( "review-baseline-classes" );
		final Path currentClasses = temporaryDirectory.resolve( "review-current-classes" );
		writeContract( baselineClasses );
		writeContract( currentClasses );
		final Path baselineMetadata = temporaryDirectory.resolve( "review-baseline.json" );
		final Path currentMetadata = temporaryDirectory.resolve( "review-current.json" );
		final ClassificationMetadataJson json = new ClassificationMetadataJson();
		Files.writeString(
				baselineMetadata,
				json.write( new ClassificationMetadata( "8.1", "8.1.9", model() ) )
		);
		Files.writeString(
				currentMetadata,
				json.write( new ClassificationMetadata( "9.0", "9.0.0", model() ) )
		);

		final MigrationReviewTask task = (MigrationReviewTask) project.getTasks().getByName( "generateMigrationReview" );
		task.getReviewFamily().set( "8.1" );
		task.getBaselineClassificationMetadataFile().set( baselineMetadata.toFile() );
		task.getCurrentClassificationMetadataFile().set( currentMetadata.toFile() );
		task.getBaselineArtifacts().setFrom( baselineClasses );
		task.getCurrentArtifacts().setFrom( currentClasses );
		task.generateReview();

		final String report = Files.readString( task.getReportFile().get().getAsFile().toPath() );
		assertTrue( report.contains( "Migration review — not compatibility enforcement" ) );
		assertTrue( report.contains( "Baseline source version: 8.1.9" ) );
		assertTrue( report.contains( "Current source version: 9.0.0" ) );
	}

	private static ClassificationModel model() {
		final String typeId = "type:fixture.Contract";
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		builder.declaration(
				typeId,
				TYPE,
				null,
				new ClassificationModel.Structure( Modifier.PUBLIC | Modifier.ABSTRACT, true, false ),
				"fixture"
		);
		builder.addClassificationOrigin(
				typeId,
				new ClassificationModel.ClassificationOrigin( API, DIRECT, typeId, Collections.emptySet() ),
				Collections.emptySet()
		);
		return builder.build();
	}

	private static void writeContract(Path root) throws Exception {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit(
				Opcodes.V17,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
				"fixture/Contract",
				null,
				"java/lang/Object",
				null
		);
		writer.visitEnd();
		final Path classFile = root.resolve( "fixture/Contract.class" );
		Files.createDirectories( classFile.getParent() );
		Files.write( classFile, writer.toByteArray() );
	}
}
