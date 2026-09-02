/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// One versioned classification metadata document.
///
/// @author Steve Ebersole
public final class ClassificationMetadata {
	public static final String SCHEMA = "hibernate-orm-classifications";
	public static final int SCHEMA_VERSION = 1;

	private final String hibernateVersion;
	private final String sourceVersion;
	private final ClassificationModel model;
	private final List<Artifact> artifacts;
	private final Set<String> artifactFileNames;

	public ClassificationMetadata(
			String hibernateVersion,
			String sourceVersion,
			ClassificationModel model) {
		this( hibernateVersion, sourceVersion, model, Collections.emptyList() );
	}

	public ClassificationMetadata(
			String hibernateVersion,
			String sourceVersion,
			ClassificationModel model,
			Collection<Artifact> artifacts) {
		this.hibernateVersion = hibernateVersion;
		this.sourceVersion = sourceVersion;
		this.model = model;
		final List<Artifact> sortedArtifacts = new ArrayList<>( artifacts );
		Collections.sort( sortedArtifacts );
		this.artifacts = Collections.unmodifiableList( sortedArtifacts );
		final Set<String> fileNames = new HashSet<>();
		for ( Artifact artifact : sortedArtifacts ) {
			fileNames.add( artifact.getFileName() );
		}
		artifactFileNames = Collections.unmodifiableSet( fileNames );
	}

	public String getHibernateVersion() {
		return hibernateVersion;
	}

	public String getSourceVersion() {
		return sourceVersion;
	}

	public ClassificationModel getModel() {
		return model;
	}

	/// Returns the exact artifacts needed to analyze the classified API and SPI
	/// declarations represented by this document.
	public List<Artifact> getArtifacts() {
		return artifacts;
	}

	/// Whether the declaration belongs to the explicitly configured migration
	/// compatibility artifact scope.
	///
	/// An empty manifest retains the complete model for in-memory analysis and
	/// legacy test fixtures. Published compatibility metadata always carries an
	/// exact, nonempty manifest.
	public boolean isMigrationCompatibilityElement(ClassificationModel.Element element) {
		if ( artifactFileNames.isEmpty() ) {
			return true;
		}
		for ( String artifact : element.getArtifact().split( "," ) ) {
			if ( artifactFileNames.contains( artifact ) ) {
				return true;
			}
		}
		return false;
	}

	/// One exact artifact in the migration-analysis class path.
	public static final class Artifact implements Comparable<Artifact> {
		private final String fileName;
		private final String group;
		private final String module;
		private final String version;
		private final boolean hibernateOrmModule;

		public Artifact(
				String fileName,
				String group,
				String module,
				String version,
				boolean hibernateOrmModule) {
			this.fileName = fileName;
			this.group = group;
			this.module = module;
			this.version = version;
			this.hibernateOrmModule = hibernateOrmModule;
		}

		public String getFileName() {
			return fileName;
		}

		public String getGroup() {
			return group;
		}

		public String getModule() {
			return module;
		}

		public String getVersion() {
			return version;
		}

		public boolean isHibernateOrmModule() {
			return hibernateOrmModule;
		}

		public String getCoordinates() {
			return group + ':' + module + ':' + version;
		}

		@Override
		public int compareTo(Artifact other) {
			int comparison = getCoordinates().compareTo( other.getCoordinates() );
			return comparison == 0 ? fileName.compareTo( other.fileName ) : comparison;
		}
	}
}
