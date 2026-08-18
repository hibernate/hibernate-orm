/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.build.OrmBuildDetails;

import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/// Generates the migration-scoped Dialect extension inventory and provisional
/// review projections.
///
/// @author Steve Ebersole
public abstract class DialectExtensionInventoryTask extends DefaultTask {
	private final Provider<IndexManager> indexManager;
	private final ConfigurableFileCollection indexedArtifacts;
	private final ConfigurableFileCollection supportDocumentationFiles;
	private final RegularFileProperty decisionOverlayFile;
	private final RegularFileProperty familyDecisionOverlayFile;
	private final DirectoryProperty outputDirectory;

	public DialectExtensionInventoryTask() {
		setGroup( TASK_GROUP_NAME );
		setDescription( "Generates the migration-scoped Dialect extension inventory" );
		indexManager = getProject().provider( () -> getProject().getExtensions().getByType( IndexManager.class ) );
		indexedArtifacts = getProject().getObjects().fileCollection();
		supportDocumentationFiles = getProject().getObjects().fileCollection();
		decisionOverlayFile = getProject().getObjects().fileProperty();
		decisionOverlayFile.convention(
				getProject().getRootProject().getLayout().getProjectDirectory()
						.file( "design/dialect-extension-decisions.tsv" )
		);
		familyDecisionOverlayFile = getProject().getObjects().fileProperty();
		familyDecisionOverlayFile.convention(
				getProject().getRootProject().getLayout().getProjectDirectory()
						.file( "design/dialect-family-decisions.tsv" )
		);
		outputDirectory = getProject().getObjects().directoryProperty();
		outputDirectory.convention(
				getProject().getLayout().getBuildDirectory().dir( "orm/reports/dialect-extension" )
		);
	}

	@InputFile
	public Provider<RegularFile> getIndexFileReference() {
		return indexManager.get().getIndexFileReferenceAccess();
	}

	@InputFile
	public Provider<RegularFile> getArtifactProvenanceFileReference() {
		return indexManager.get().getArtifactFileReferenceAccess();
	}

	@Classpath
	public ConfigurableFileCollection getIndexedArtifacts() {
		return indexedArtifacts;
	}

	@InputFiles
	public ConfigurableFileCollection getSupportDocumentationFiles() {
		return supportDocumentationFiles;
	}

	@Optional
	@InputFile
	public Provider<RegularFile> getDecisionOverlayFileReference() {
		return decisionOverlayFile;
	}

	@InputFile
	public Provider<RegularFile> getFamilyDecisionOverlayFileReference() {
		return familyDecisionOverlayFile;
	}

	@OutputDirectory
	public Provider<Directory> getOutputDirectoryReference() {
		return outputDirectory;
	}

	@TaskAction
	public void generateInventory() {
		final IndexManager indexes = indexManager.get();
		final ClassificationModel classifications = new JandexClassificationClassifier( indexes::getArtifact )
				.classify( indexes.getIndex() );
		final DialectExtensionInventory inventory = new DialectExtensionInventoryAnalyzer().analyze(
				indexes.getIndex(),
				classifications,
				new BytecodeLinkageAnalyzer().analyze( indexedArtifacts.getFiles() ),
				Helper.asClassLoader( indexedArtifacts ),
				supportDocumentationFiles.getFiles()
		);
		final OrmBuildDetails buildDetails = getProject().getExtensions().getByType( OrmBuildDetails.class );
		final DialectExtensionInventoryRenderer renderer = new DialectExtensionInventoryRenderer();
		final DialectExtensionDecisionOverlay decisions = decisionOverlayFile.isPresent()
				? DialectExtensionDecisionOverlay.read( decisionOverlayFile.get().getAsFile().toPath() )
				: DialectExtensionDecisionOverlay.empty();
		final DialectFamilyDecisionOverlay familyDecisions = DialectFamilyDecisionOverlay.read(
				familyDecisionOverlayFile.get().getAsFile().toPath()
		);
		final Path output = outputDirectory.get().getAsFile().toPath();
		write(
				output.resolve( "dialect-extension-inventory.json" ),
				renderer.json(
						inventory,
						buildDetails.getHibernateVersionFamily(),
						buildDetails.getHibernateVersionName()
				)
		);
		write( output.resolve( "dialect-extension-decisions.tsv" ), renderer.decisionOverlay( inventory, decisions ) );
		write( output.resolve( "dialect-extension-summary.adoc" ), renderer.summary( inventory ) );
		write( output.resolve( "dialect-selection-matrices.adoc" ), renderer.selectionMatrices( inventory ) );
		write(
				output.resolve( "dialect-family-inventory.adoc" ),
				renderer.familyInventory( inventory, familyDecisions )
		);
		write(
				output.resolve( "dialect-extension-review.adoc" ),
				renderer.review(
						inventory,
						decisions,
						buildDetails.getHibernateVersionFamily(),
						buildDetails.getHibernateVersionName()
				)
		);
	}

	private static void write(Path path, String contents) {
		try {
			Files.createDirectories( path.getParent() );
			Files.writeString( path, contents, StandardCharsets.UTF_8 );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to write Dialect extension inventory " + path, e );
		}
	}
}
