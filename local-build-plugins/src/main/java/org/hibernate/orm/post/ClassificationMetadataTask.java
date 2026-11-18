package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/// Generates the canonical `classifications.json` metadata document.
///
/// @author Steve Ebersole
public abstract class ClassificationMetadataTask extends AbstractJandexAwareTask {
	private final RegularFileProperty metadataFile;
	private final RegularFileProperty compressedMetadataFile;
	private final ConfigurableFileCollection migrationCompatibilityArtifacts;

	public ClassificationMetadataTask() {
		setDescription( "Generates the canonical Hibernate classification metadata" );
		metadataFile = getProject().getObjects().fileProperty();
		metadataFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/classifications.json" )
		);
		compressedMetadataFile = getProject().getObjects().fileProperty();
		compressedMetadataFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/classifications.json.gz" )
		);
		migrationCompatibilityArtifacts = getProject().getObjects().fileCollection();
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return metadataFile;
	}

	@OutputFile
	public Provider<RegularFile> getCompressedMetadataFileReference() {
		return compressedMetadataFile;
	}

	/// The explicitly configured artifact scope for migration compatibility.
	@Classpath
	public ConfigurableFileCollection getMigrationCompatibilityArtifacts() {
		return migrationCompatibilityArtifacts;
	}

	@Input
	public abstract Property<String> getHibernateVersionFamily();

	@Input
	public abstract Property<String> getHibernateVersionName();

	@TaskAction
	public void generateClassificationMetadata() {
		final IndexManager indexManager = getIndexManager();
		final ClassificationModel model = new JandexClassificationClassifier( indexManager::getArtifact )
				.classify( indexManager.getIndex() );
		final ClassificationMetadata metadata = new ClassificationMetadata(
				getHibernateVersionFamily().get(),
				getHibernateVersionName().get(),
				model,
				indexManager.getClassificationArtifacts(
						model,
						getHibernateVersionName().get(),
						migrationCompatibilityArtifacts.getFiles()
				)
		);
		final ClassificationMetadataJson json = new ClassificationMetadataJson();
		final String serialized = json.write( metadata );
		write( getReportFileReference().get().getAsFile(), serialized.getBytes( StandardCharsets.UTF_8 ) );
		write( compressedMetadataFile.get().getAsFile(), json.gzip( serialized ) );
	}

	private static void write(File file, byte[] contents) {
		try {
			Files.createDirectories( file.toPath().getParent() );
			Files.write( file.toPath(), contents );
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to write classification metadata " + file.getAbsolutePath(), e );
		}
	}
}
