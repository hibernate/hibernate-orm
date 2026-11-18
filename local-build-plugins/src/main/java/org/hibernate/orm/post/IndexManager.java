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

import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RelativePath;
import org.gradle.api.logging.Logger;
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
	private final Provider<RegularFile> indexFileReferenceAccess;
	private final Provider<RegularFile> artifactFileReferenceAccess;
	private final Provider<RegularFile> descriptorFileReferenceAccess;

	private Index index;
	private Map<String, TreeSet<String>> artifactsByElement;
	private Map<String, ClassificationMetadata.Artifact> artifactDescriptors;

	/// Creates a manager over the files produced by [IndexerTask]. Holds no reference to the
	/// Gradle model, so it is safe to create from a task action under the configuration cache.
	public IndexManager(
			Provider<RegularFile> indexFileReferenceAccess,
			Provider<RegularFile> artifactFileReferenceAccess,
			Provider<RegularFile> descriptorFileReferenceAccess) {
		this.indexFileReferenceAccess = indexFileReferenceAccess;
		this.artifactFileReferenceAccess = artifactFileReferenceAccess;
		this.descriptorFileReferenceAccess = descriptorFileReferenceAccess;
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
			artifactDescriptors = loadArtifactDescriptors( descriptorFileReferenceAccess );
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


	private static Map<String, ClassificationMetadata.Artifact> loadArtifactDescriptors(
			Provider<RegularFile> descriptorFileReferenceAccess) {
		final File descriptorFile = descriptorFileReferenceAccess.get().getAsFile();
		if ( !descriptorFile.exists() ) {
			throw new IllegalStateException( "Cannot load artifact coordinates; the stored file does not exist - " + descriptorFile.getAbsolutePath() );
		}
		final Map<String, ClassificationMetadata.Artifact> descriptors = new TreeMap<>();
		try {
			for ( String line : Files.readAllLines( descriptorFile.toPath() ) ) {
				final String[] parts = line.split( "\t", -1 );
				if ( parts.length != 5 ) {
					throw new IllegalStateException( "Malformed artifact coordinates: " + line );
				}
				descriptors.put(
						parts[0],
						new ClassificationMetadata.Artifact( parts[0], parts[1], parts[2], parts[3], Boolean.parseBoolean( parts[4] ) )
				);
			}
			return descriptors;
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to read artifact coordinates - " + descriptorFile.getAbsolutePath(), e );
		}
	}

	/**
	 * Used from {@link IndexerTask} as its action
	 */
	void index(
			Map<File, ClassificationMetadata.Artifact> descriptors,
			ArchiveOperations archiveOperations,
			Logger logger) {
		if ( index != null ) {
			throw new IllegalStateException( "Index was already created or loaded" );
		}

		final Indexer indexer = new Indexer();
		artifactsByElement = new TreeMap<>();

		// note: each of `artifacts` is a jar-file
		artifactDescriptors = new TreeMap<>();
		final Set<File> artifacts = new TreeSet<>();
		descriptors.forEach( (file, descriptor) -> {
			artifacts.add( file );
			final ClassificationMetadata.Artifact previous = artifactDescriptors.put( file.getName(), descriptor );
			if ( previous != null && !previous.getCoordinates().equals( descriptor.getCoordinates() ) ) {
				throw new IllegalStateException( "Artifact file name maps to multiple coordinates: " + file.getName() );
			}
		} );

		artifacts.forEach( (jar) -> {
			final String artifactName = jar.getName();
			final FileTree jarFileTree = archiveOperations.zipTree( jar );
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
										logger.lifecycle( "Problem indexing class file - {}", details.getFile().getAbsolutePath() );
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
		storeDescriptors();
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

	private void storeDescriptors() {
		final File descriptorFile = prepareOutputFile( descriptorFileReferenceAccess );
		try ( final FileWriter fileWriter = new FileWriter( descriptorFile ) ) {
			for ( ClassificationMetadata.Artifact artifact : artifactDescriptors.values() ) {
				fileWriter.write( String.join( "\t",
						artifact.getFileName(),
						artifact.getGroup(),
						artifact.getModule(),
						artifact.getVersion(),
						Boolean.toString( artifact.isHibernateOrmModule() ) ) );
				fileWriter.write( '\n' );
			}
		}
		catch (IOException e) {
			throw new RuntimeException( "Error writing artifact coordinates - " + descriptorFile.getAbsolutePath(), e );
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
