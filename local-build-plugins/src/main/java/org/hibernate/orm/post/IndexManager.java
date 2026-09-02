/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RelativePath;
import org.gradle.api.provider.Provider;

import org.jboss.jandex.ClassSummary;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;

/**
 * Encapsulates and manages a Jandex Index
 *
 * @author Steve Ebersole
 */
public class IndexManager {
	private final Configuration artifactsToProcess;
	private final Provider<RegularFile> indexFileReferenceAccess;
	private final Provider<RegularFile> artifactFileReferenceAccess;
	private final Project project;

	private Index index;
	private Map<String, TreeSet<String>> artifactsByElement;
	private Map<String, ClassificationMetadata.Artifact> artifactDescriptors;

	public IndexManager(Configuration artifactsToProcess, Project project) {
		this.artifactsToProcess = artifactsToProcess;
		this.indexFileReferenceAccess = project.getLayout()
				.getBuildDirectory()
				.file( "orm/reports/indexing/jandex.idx" );
		this.artifactFileReferenceAccess = project.getLayout()
				.getBuildDirectory()
				.file( "orm/reports/indexing/artifacts.txt" );
		this.project = project;
	}

	public Configuration getArtifactsToProcess() {
		return artifactsToProcess;
	}

	public Provider<RegularFile> getIndexFileReferenceAccess() {
		return indexFileReferenceAccess;
	}

	public Provider<RegularFile> getArtifactFileReferenceAccess() {
		return artifactFileReferenceAccess;
	}

	public Index getIndex() {
		if ( index == null ) {
			index = loadIndex( indexFileReferenceAccess );
			artifactsByElement = loadArtifacts( artifactFileReferenceAccess );
		}
		return index;
	}

	public String getArtifact(String elementId) {
		getIndex();
		final String lookupId;
		if ( elementId.startsWith( "package:" ) || elementId.startsWith( "type:" ) ) {
			lookupId = elementId;
		}
		else {
			final int typeStart = elementId.indexOf( ':' ) + 1;
			final int memberSeparator = elementId.indexOf( '#', typeStart );
			lookupId = memberSeparator < 0
					? elementId
					: "type:" + elementId.substring( typeStart, memberSeparator );
		}
		final Set<String> artifacts = artifactsByElement.get( lookupId );
		return artifacts == null || artifacts.isEmpty() ? "unknown" : String.join( ",", artifacts );
	}

	/// Returns the resolvable artifacts owning API or SPI declarations.
	public List<ClassificationMetadata.Artifact> getClassificationArtifacts(
			ClassificationModel model,
			String sourceVersion,
			Collection<File> migrationCompatibilityArtifacts) {
		getIndex();
		if ( artifactDescriptors == null ) {
			artifactDescriptors = resolveArtifactDescriptors();
		}
		final Set<String> requiredFiles = new TreeSet<>();
		final Set<String> compatibilityFiles = new TreeSet<>();
		for ( File artifact : migrationCompatibilityArtifacts ) {
			compatibilityFiles.add( artifact.getName() );
		}
		for ( ClassificationModel.Element element : model.getElements() ) {
			if ( element.getCategory() == ClassificationModel.Category.API
					|| element.getCategory() == ClassificationModel.Category.SPI ) {
				for ( String artifact : element.getArtifact().split( "," ) ) {
					requiredFiles.add( artifact );
				}
			}
		}
		final List<ClassificationMetadata.Artifact> result = new ArrayList<>();
		for ( String fileName : requiredFiles ) {
			if ( !compatibilityFiles.contains( fileName ) ) {
				continue;
			}
			final ClassificationMetadata.Artifact artifact = artifactDescriptors.get( fileName );
			if ( artifact == null ) {
				throw new IllegalStateException( "No resolvable coordinates for classified artifact " + fileName );
			}
			if ( artifact.isHibernateOrmModule() && !sourceVersion.equals( artifact.getVersion() ) ) {
				throw new IllegalStateException(
						"Hibernate ORM artifact " + artifact.getCoordinates()
								+ " does not use source version " + sourceVersion
				);
			}
			result.add( artifact );
		}
		result.sort( ClassificationMetadata.Artifact::compareTo );
		return result;
	}

	private static Index loadIndex(Provider<RegularFile> indexFileReferenceAccess) {
		final File indexFile = indexFileReferenceAccess.get().getAsFile();
		if ( !indexFile.exists() ) {
			throw new IllegalStateException( "Cannot load index; the stored file does not exist - " + indexFile.getAbsolutePath() );
		}

		try ( final FileInputStream stream = new FileInputStream( indexFile ) ) {
			final IndexReader indexReader = new IndexReader( stream );
			return indexReader.read();
		}
		catch (FileNotFoundException e) {
			throw new IllegalStateException( "Cannot load index; the stored file does not exist - " + indexFile.getAbsolutePath(), e );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Cannot load index; unable to read stored file - " + indexFile.getAbsolutePath(), e );
		}
	}

	private static Map<String, TreeSet<String>> loadArtifacts(Provider<RegularFile> artifactFileReferenceAccess) {
		final File artifactFile = artifactFileReferenceAccess.get().getAsFile();
		if ( !artifactFile.exists() ) {
			throw new IllegalStateException( "Cannot load indexed artifact provenance; the stored file does not exist - " + artifactFile.getAbsolutePath() );
		}
		final Map<String, TreeSet<String>> artifacts = new TreeMap<>();
		try {
			for ( String line : Files.readAllLines( artifactFile.toPath() ) ) {
				final int separator = line.indexOf( '\t' );
				if ( separator <= 0 || separator == line.length() - 1 ) {
					throw new IllegalStateException( "Malformed indexed artifact provenance: " + line );
				}
				artifacts.computeIfAbsent( line.substring( 0, separator ), (key) -> new TreeSet<>() )
						.add( line.substring( separator + 1 ) );
			}
			return artifacts;
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to read indexed artifact provenance - " + artifactFile.getAbsolutePath(), e );
		}
	}


	/**
	 * Used from {@link IndexerTask} as its action
	 */
	void index() {
		if ( index != null ) {
			throw new IllegalStateException( "Index was already created or loaded" );
		}

		final Indexer indexer = new Indexer();
		artifactsByElement = new TreeMap<>();

		// note: each of `artifacts` is a jar-file
		artifactDescriptors = new TreeMap<>();
		final Set<ResolvedArtifactResult> resolvedArtifacts = artifactsToProcess.getIncoming().getArtifacts().getArtifacts();
		final Set<File> artifacts = new TreeSet<>();
		for ( ResolvedArtifactResult resolvedArtifact : resolvedArtifacts ) {
			final File file = resolvedArtifact.getFile();
			artifacts.add( file );
			final ClassificationMetadata.Artifact descriptor = descriptor( resolvedArtifact );
			final ClassificationMetadata.Artifact previous = artifactDescriptors.put( file.getName(), descriptor );
			if ( previous != null && !previous.getCoordinates().equals( descriptor.getCoordinates() ) ) {
				throw new IllegalStateException( "Artifact file name maps to multiple coordinates: " + file.getName() );
			}
		}

		artifacts.forEach( (jar) -> {
			final String artifactName = jar.getName();
			final FileTree jarFileTree = project.zipTree( jar );
			jarFileTree.visit(
					new FileVisitor() {
						private boolean isInOrmPackage(RelativePath relativePath) {
							return relativePath.getPathString().startsWith( "org/hibernate/" );
						}

						@Override
						public void visitDir(FileVisitDetails details) {
							// No directory-level metadata is required by the canonical classifier.
						}

						@Override
						public void visitFile(FileVisitDetails details) {
							final RelativePath relativePath = details.getRelativePath();
							if ( !isInOrmPackage( relativePath ) ) {
								return;
							}

							if ( relativePath.getPathString().endsWith( ".class" ) ) {
								try (final FileInputStream stream = new FileInputStream( details.getFile() )) {
									final ClassSummary classSummary = indexer.indexWithSummary( stream );
									if ( classSummary == null ) {
										project.getLogger()
												.lifecycle( "Problem indexing class file - " + details.getFile()
														.getAbsolutePath() );
									}
									else {
										final String className = classSummary.name().toString();
										recordArtifact( "type:" + className, artifactName );
										final int separator = className.lastIndexOf( '.' );
										if ( separator > 0 ) {
											recordArtifact( "package:" + className.substring( 0, separator ), artifactName );
										}
									}
								}
								catch (IllegalArgumentException e) {
									throw new RuntimeException( "Problem indexing class file - " + details.getFile()
											.getAbsolutePath(), e );
								}
								catch (FileNotFoundException e) {
									throw new RuntimeException( "Problem locating project class file - " + details.getFile()
											.getAbsolutePath(), e );
								}
								catch (IOException e) {
									throw new RuntimeException( "Error accessing project class file - " + details.getFile()
											.getAbsolutePath(), e );
								}
							}
						}
					}
			);
		} );

		this.index = indexer.complete();
		storeIndex();
		storeArtifacts();
	}

	private Map<String, ClassificationMetadata.Artifact> resolveArtifactDescriptors() {
		final Map<String, ClassificationMetadata.Artifact> descriptors = new TreeMap<>();
		for ( ResolvedArtifactResult resolvedArtifact : artifactsToProcess.getIncoming().getArtifacts().getArtifacts() ) {
			final ClassificationMetadata.Artifact descriptor = descriptor( resolvedArtifact );
			final ClassificationMetadata.Artifact previous = descriptors.put(
					resolvedArtifact.getFile().getName(),
					descriptor
			);
			if ( previous != null && !previous.getCoordinates().equals( descriptor.getCoordinates() ) ) {
				throw new IllegalStateException(
						"Artifact file name maps to multiple coordinates: " + resolvedArtifact.getFile().getName()
				);
			}
		}
		return descriptors;
	}

	private ClassificationMetadata.Artifact descriptor(ResolvedArtifactResult artifact) {
		if ( artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier ) {
			final ModuleComponentIdentifier module = (ModuleComponentIdentifier) artifact.getId().getComponentIdentifier();
			return new ClassificationMetadata.Artifact(
					artifact.getFile().getName(),
					module.getGroup(),
					module.getModule(),
					module.getVersion(),
					false
			);
		}
		if ( artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier ) {
			final ProjectComponentIdentifier identifier = (ProjectComponentIdentifier) artifact.getId().getComponentIdentifier();
			final Project owner = project.getRootProject().findProject( identifier.getProjectPath() );
			if ( owner == null ) {
				throw new IllegalStateException( "Unable to locate artifact project " + identifier.getProjectPath() );
			}
			return new ClassificationMetadata.Artifact(
					artifact.getFile().getName(),
					owner.getGroup().toString(),
					owner.getName(),
					owner.getVersion().toString(),
					true
			);
		}
		throw new IllegalStateException(
				"Classified artifact does not have module coordinates: " + artifact.getFile()
		);
	}

	private void recordArtifact(String elementId, String artifactName) {
		artifactsByElement.computeIfAbsent( elementId, (key) -> new TreeSet<>() ).add( artifactName );
	}

	private void storeIndex() {
		final File indexFile = prepareOutputFile( indexFileReferenceAccess );

		try ( final FileOutputStream stream = new FileOutputStream( indexFile ) ) {
			final IndexWriter indexWriter = new IndexWriter( stream );
			indexWriter.write( index );
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Should never happen", e );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error accessing index file - " + indexFile.getAbsolutePath(), e );
		}
	}

	private void storeArtifacts() {
		final File artifactFile = prepareOutputFile( artifactFileReferenceAccess );
		try ( final FileWriter fileWriter = new FileWriter( artifactFile ) ) {
			for ( Map.Entry<String, TreeSet<String>> entry : artifactsByElement.entrySet() ) {
				for ( String artifact : entry.getValue() ) {
					fileWriter.write( entry.getKey() );
					fileWriter.write( '\t' );
					fileWriter.write( artifact );
					fileWriter.write( '\n' );
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException( "Error writing indexed artifact provenance - " + artifactFile.getAbsolutePath(), e );
		}
	}

	private File prepareOutputFile(Provider<RegularFile> outputFileReferenceAccess) {
		final File outputFile = outputFileReferenceAccess.get().getAsFile();
		if ( outputFile.exists() ) {
			outputFile.delete();
		}

		try {
			outputFile.getParentFile().mkdirs();
			outputFile.createNewFile();
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to create index file - " + outputFile.getAbsolutePath(), e );
		}

		return outputFile;
	}
}
