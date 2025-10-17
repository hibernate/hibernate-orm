/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RelativePath;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.jboss.jandex.ClassSummary;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Encapsulates and manages a Jandex Index
 *
 * @author Steve Ebersole
 */
public class IndexManager {
	private final ConfigurableFileCollection artifactsToProcess;
	private final Provider<RegularFile> indexFileReferenceAccess;
	private final Provider<RegularFile> packageFileReferenceAccess;
	private Index index;
	private TreeSet<Inclusion> internalPackageNames;

	@Inject
	public IndexManager(ObjectFactory objects, ProjectLayout layout) {
		this.artifactsToProcess = objects.fileCollection();
		this.indexFileReferenceAccess = layout
				.getBuildDirectory()
				.file( "orm/reports/indexing/jandex.idx" );
		this.packageFileReferenceAccess = layout
				.getBuildDirectory()
				.file( "orm/reports/indexing/internal-packages.txt" );
	}


	@InputFiles
	@SkipWhenEmpty
	public ConfigurableFileCollection getArtifactsToProcess() {
		return artifactsToProcess;
	}

	@OutputFile
	public Provider<RegularFile> getIndexFileReferenceAccess() {
		return indexFileReferenceAccess;
	}

	@OutputFile
	public Provider<RegularFile> getPackageFileReferenceAccess() {
		return packageFileReferenceAccess;
	}

	@Internal
	public TreeSet<Inclusion> getInternalPackageNames() {
		return internalPackageNames;
	}

	@Internal
	public Index getIndex() {
		if ( index == null ) {
			index = loadIndex( indexFileReferenceAccess );
			internalPackageNames = loadInternalPackageNames( packageFileReferenceAccess );
		}
		return index;
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

	private static TreeSet<Inclusion> loadInternalPackageNames(Provider<RegularFile> packageFileReferenceAccess) {
		final File packageNameFile = packageFileReferenceAccess.get().getAsFile();
		if ( !packageNameFile.exists() ) {
			throw new IllegalStateException( "Cannot load internal packages; the stored file does not exist - " + packageNameFile.getAbsolutePath() );
		}

		final TreeSet<Inclusion> inclusions = new TreeSet<>( Comparator.comparing( Inclusion::getPath ) );
		try {
			final List<String> lines = Files.readAllLines( packageNameFile.toPath() );
			lines.forEach( (line) -> {
				if ( line == null || line.isEmpty() ) {
					return;
				}

				inclusions.add( new Inclusion( line, true ) );
			} );
			return inclusions;
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to read package-name file - " + packageNameFile.getAbsolutePath(), e );
		}
	}


	/**
	 * Used from {@link IndexerTask} as its action
	 */
	void index(ArchiveOperations archiveOperations, Logger logger) {
		if ( index != null ) {
			throw new IllegalStateException( "Index was already created or loaded" );
		}

		final Indexer indexer = new Indexer();
		internalPackageNames = new TreeSet<>( Comparator.comparing( Inclusion::getPath ) );

		// note: each of `artifacts` is a jar-file
		final Set<File> artifacts = artifactsToProcess.getFiles();

		artifacts.forEach( (jar) -> {
			final FileTree jarFileTree = archiveOperations.zipTree( jar );
			jarFileTree.visit(
					new FileVisitor() {
						private boolean isInOrmPackage(RelativePath relativePath) {
							return relativePath.getPathString().startsWith( "org/hibernate/" );
						}

						@Override
						public void visitDir(FileVisitDetails details) {
							final RelativePath relativePath = details.getRelativePath();
							if ( !isInOrmPackage( relativePath ) ) {
								return;
							}

							if ( relativePath.getPathString().endsWith( "internal" )
									|| relativePath.getPathString().endsWith( "internal/" ) ) {
								final String packageName = relativePath.toString().replace( '/', '.' );
								internalPackageNames.add( new Inclusion( packageName, true ) );
							}
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
										logger.lifecycle( "Problem indexing class file - {}", details.getFile()
														.getAbsolutePath() );
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
		storePackageNames();
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

	private void storePackageNames() {
		final File packageNameFile = prepareOutputFile( packageFileReferenceAccess );

		try ( final FileWriter fileWriter = new FileWriter( packageNameFile ) ) {
			internalPackageNames.forEach( (inclusion) -> {
				try {
					fileWriter.write( inclusion.getPath() );
					fileWriter.write( '\n' );
				}
				catch (IOException e) {
					throw new RuntimeException( "Unable to write to package-name file - " + packageNameFile.getAbsolutePath(), e );
				}
			} );
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Should never happen", e );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error accessing package-name file - " + packageNameFile.getAbsolutePath(), e );
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
