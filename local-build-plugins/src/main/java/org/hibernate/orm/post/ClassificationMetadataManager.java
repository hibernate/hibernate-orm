package org.hibernate.orm.post;

import java.nio.file.Path;
import java.util.function.Function;
import javax.inject.Inject;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/// Lazily parses and shares the immutable canonical classification metadata
/// within one Gradle project execution.
///
/// Registered as a [BuildService] (one per project) so the tasks share the parsed model
/// without holding a reference to the Gradle model, as required by the configuration cache.
///
/// @author Steve Ebersole
public class ClassificationMetadataManager implements BuildService<BuildServiceParameters.None> {
	private final Function<Path, ClassificationMetadata> reader;
	private Path source;
	private ClassificationMetadata metadata;

	@Inject
	public ClassificationMetadataManager() {
		this( (path) -> new ClassificationMetadataJson().read( path ) );
	}

	ClassificationMetadataManager(Function<Path, ClassificationMetadata> reader) {
		this.reader = reader;
	}

	@Override
	public BuildServiceParameters.None getParameters() {
		return null;
	}

	/// Returns the single parsed model for this project execution.
	///
	/// Initialization is synchronized because report and validation tasks may
	/// execute concurrently.
	public synchronized ClassificationMetadata getMetadata(Path metadataFile) {
		final Path normalizedPath = metadataFile.toAbsolutePath().normalize();
		if ( metadata == null ) {
			source = normalizedPath;
			metadata = reader.apply( normalizedPath );
		}
		else if ( !source.equals( normalizedPath ) ) {
			throw new IllegalArgumentException(
					"Classification metadata manager already loaded " + source + "; cannot also load " + normalizedPath
			);
		}
		return metadata;
	}
}
