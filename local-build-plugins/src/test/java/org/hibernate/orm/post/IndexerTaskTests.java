package org.hibernate.orm.post;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.logging.Logging;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests how the aggregated index, its artifact provenance and artifact coordinates are
/// produced and shared with the tasks reading them.
class IndexerTaskTests {

	@Test
	void coordinatesRoundTripThroughTheirEncodedForm() {
		final ClassificationMetadata.Artifact module = IndexerTask.decode(
				"hibernate-core-8.0.0.jar",
				IndexerTask.encode( "org.hibernate.orm", "hibernate-core", "8.0.0", true )
		);
		assertEquals( "hibernate-core-8.0.0.jar", module.getFileName() );
		assertEquals( "org.hibernate.orm", module.getGroup() );
		assertEquals( "hibernate-core", module.getModule() );
		assertEquals( "8.0.0", module.getVersion() );
		assertTrue( module.isHibernateOrmModule() );

		final ClassificationMetadata.Artifact external = IndexerTask.decode(
				"jakarta.persistence-api-3.2.0.jar",
				IndexerTask.encode( "jakarta.persistence", "jakarta.persistence-api", "3.2.0", false )
		);
		assertFalse( external.isHibernateOrmModule() );
	}

	@Test
	void malformedCoordinatesAreRejected() {
		assertThrows( IllegalStateException.class, () -> IndexerTask.decode( "a.jar", "only:three:parts" ) );
	}

	@Test
	void indexingStoresIndexProvenanceAndCoordinates(@TempDir Path temporaryDirectory) throws IOException {
		final Path jar = createJarWithClass( temporaryDirectory, "org.hibernate.fixture.Fixture" );
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		// only `zipTree` is needed; delegate it to the project
		final ArchiveOperations archiveOperations = (ArchiveOperations) Proxy.newProxyInstance(
				ArchiveOperations.class.getClassLoader(),
				new Class<?>[] { ArchiveOperations.class },
				(proxy, method, args) -> {
					if ( method.getName().equals( "zipTree" ) ) {
						return project.zipTree( args[0] );
					}
					throw new UnsupportedOperationException( method.getName() );
				}
		);

		final var layout = project.getLayout();
		final var indexFile = layout.getBuildDirectory().file( "indexing/jandex.idx" );
		final var artifactFile = layout.getBuildDirectory().file( "indexing/artifacts.txt" );
		final var descriptorFile = layout.getBuildDirectory().file( "indexing/artifact-coordinates.txt" );

		new IndexManager( indexFile, artifactFile, descriptorFile ).index(
				Map.of(
						jar.toFile(),
						new ClassificationMetadata.Artifact( jar.getFileName().toString(), "org.hibernate.orm", "fixture", "1.0", true )
				),
				archiveOperations,
				Logging.getLogger( IndexerTaskTests.class )
		);

		// a fresh manager only knows the files, as the tasks reading the index do
		final IndexManager reader = new IndexManager( indexFile, artifactFile, descriptorFile );
		assertTrue( reader.getIndex().getKnownClasses().stream()
				.anyMatch( (c) -> c.name().toString().equals( "org.hibernate.fixture.Fixture" ) ) );
		assertEquals( jar.getFileName().toString(), reader.getArtifact( "type:org.hibernate.fixture.Fixture" ) );
		assertEquals( jar.getFileName().toString(), reader.getArtifact( "package:org.hibernate.fixture" ) );
		assertEquals(
				List.of( jar.getFileName() + "\torg.hibernate.orm\tfixture\t1.0\ttrue" ),
				Files.readAllLines( descriptorFile.get().getAsFile().toPath() )
		);
	}

	@Test
	void jandexAwareTasksReadTheIndexerOutputs() {
		final Project project = ProjectBuilder.builder().build();
		new ReportGenerationPlugin().apply( project );

		final IndexerTask index = (IndexerTask) project.getTasks().getByName( "buildAggregatedIndex" );
		final LoggingReportTask logging = (LoggingReportTask) project.getTasks().getByName( "generateLoggingReport" );
		final Task dialect = project.getTasks().getByName( "generateDialectTableReport" );

		assertEquals( index.getIndexFile().get(), logging.getIndexFile().get() );
		assertEquals( index.getArtifactFile().get(), logging.getArtifactFile().get() );
		assertEquals( index.getDescriptorFile().get(), logging.getDescriptorFile().get() );
		assertTrue( logging.getTaskDependencies().getDependencies( logging ).contains( index ) );
		assertTrue( dialect.getTaskDependencies().getDependencies( dialect ).contains( index ) );
	}

	private static Path createJarWithClass(Path directory, String className) throws IOException {
		final Path sources = Files.createDirectories( directory.resolve( "src" ) );
		final Path classes = Files.createDirectories( directory.resolve( "classes" ) );
		final String simpleName = className.substring( className.lastIndexOf( '.' ) + 1 );
		final String packageName = className.substring( 0, className.lastIndexOf( '.' ) );
		final Path sourceFile = sources.resolve( simpleName + ".java" );
		Files.writeString(
				sourceFile,
				"package " + packageName + "; public class " + simpleName + " {}",
				StandardCharsets.UTF_8
		);
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertEquals( 0, compiler.run( null, null, null, "-d", classes.toString(), sourceFile.toString() ) );

		final Path jar = directory.resolve( "fixture.jar" );
		try ( OutputStream out = Files.newOutputStream( jar ); JarOutputStream jarOut = new JarOutputStream( out ) ) {
			final String entry = className.replace( '.', '/' ) + ".class";
			jarOut.putNextEntry( new JarEntry( entry ) );
			jarOut.write( Files.readAllBytes( classes.resolve( entry ) ) );
			jarOut.closeEntry();
		}
		return jar;
	}
}
