/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.nio.file.Path;
import java.util.function.Function;

/// Lazily parses and shares the immutable canonical classification metadata
/// within one Gradle project execution.
///
/// @author Steve Ebersole
public final class ClassificationMetadataManager {
	private final Function<Path, ClassificationMetadata> reader;
	private Path source;
	private ClassificationMetadata metadata;

	public ClassificationMetadataManager() {
		this( (path) -> new ClassificationMetadataJson().read( path ) );
	}

	ClassificationMetadataManager(Function<Path, ClassificationMetadata> reader) {
		this.reader = reader;
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
