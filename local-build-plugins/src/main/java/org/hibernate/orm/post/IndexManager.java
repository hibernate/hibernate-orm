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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
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
		final Set<File> artifacts = artifactsToProcess.resolve();

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
