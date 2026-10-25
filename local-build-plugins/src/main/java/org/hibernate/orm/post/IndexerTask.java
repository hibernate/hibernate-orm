package org.hibernate.orm.post;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import static org.hibernate.orm.post.ReportGenerationPlugin.AGGREGATE_CONFIG_NAME;
import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/**
 * Task for creating Jandex Index within Gradle UP-TO-DATE handling
 *
 * @author Steve Ebersole
 */
public abstract class IndexerTask extends DefaultTask {
	private final ArchiveOperations archiveOperations;

	@Inject
	public IndexerTask(ArchiveOperations archiveOperations) {
		this.archiveOperations = archiveOperations;
		setGroup( TASK_GROUP_NAME );
		setDescription( String.format( "Builds a Jandex Index from the artifacts attached to the `%s` Configuration", AGGREGATE_CONFIG_NAME ) );
	}

	/// The jars to index.
	@Classpath
	@SkipWhenEmpty
	public abstract ConfigurableFileCollection getArtifactsToProcess();

	/// Coordinates of the jars in [#getArtifactsToProcess], keyed by file name and encoded as
	/// `group:module:version:hibernateOrmModule`.  Resolved when the task graph is built so
	/// that the action never needs access to the Gradle model.
	@Input
	public abstract MapProperty<String, String> getArtifactCoordinates();

	@OutputFile
	public abstract RegularFileProperty getIndexFile();

	@OutputFile
	public abstract RegularFileProperty getArtifactFile();

	@OutputFile
	public abstract RegularFileProperty getDescriptorFile();

	@TaskAction
	public void createIndex() {
		final Map<String, String> coordinates = getArtifactCoordinates().get();
		final Map<File, ClassificationMetadata.Artifact> descriptors = new TreeMap<>();
		for ( File jar : getArtifactsToProcess().getFiles() ) {
			final String encoded = coordinates.get( jar.getName() );
			if ( encoded == null ) {
				throw new IllegalStateException( "No resolvable coordinates for classified artifact " + jar.getName() );
			}
			descriptors.put( jar, decode( jar.getName(), encoded ) );
		}
		new IndexManager( getIndexFile(), getArtifactFile(), getDescriptorFile() )
				.index( descriptors, archiveOperations, getLogger() );
	}

	static String encode(String group, String module, String version, boolean hibernateOrmModule) {
		return String.join( ":", group, module, version, Boolean.toString( hibernateOrmModule ) );
	}

	private static ClassificationMetadata.Artifact decode(String fileName, String encoded) {
		final String[] parts = encoded.split( ":", -1 );
		if ( parts.length != 4 ) {
			throw new IllegalStateException( "Malformed artifact coordinates for " + fileName + ": " + encoded );
		}
		return new ClassificationMetadata.Artifact( fileName, parts[0], parts[1], parts[2], Boolean.parseBoolean( parts[3] ) );
	}
}
