package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;

import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJandexAwareTask extends DefaultTask {
	public AbstractJandexAwareTask() {
		setGroup( TASK_GROUP_NAME );
	}

	@Internal
	protected abstract Provider<RegularFile> getTaskReportFileReference();

	@Input
	public abstract Property<String> getHibernateVersion();

	@InputFile
	public abstract RegularFileProperty getIndexFile();

	@InputFile
	public abstract RegularFileProperty getArtifactFile();

	@InputFile
	public abstract RegularFileProperty getDescriptorFile();

	@Internal
	protected IndexManager getIndexManager() {
		return new IndexManager( getIndexFile(), getArtifactFile(), getDescriptorFile() );
	}

	@OutputFile
	public Provider<RegularFile> getReportFileReference() {
		return getTaskReportFileReference();
	}

	protected File prepareReportFile() {
		final File reportFile = getReportFileReference().get().getAsFile();

		if ( reportFile.getParentFile().exists() ) {
			if ( reportFile.exists() ) {
				if ( !reportFile.delete() ) {
					throw new RuntimeException( "Unable to delete report file - " + reportFile.getAbsolutePath() );
				}
			}
		}
		else {
			if ( !reportFile.getParentFile().mkdirs() ) {
				throw new RuntimeException( "Unable to create report file directories - " + reportFile.getAbsolutePath() );
			}
		}

		try {
			if ( !reportFile.createNewFile() ) {
				throw new RuntimeException( "Unable to create report file - " + reportFile.getAbsolutePath() );
			}
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to create report file - " + reportFile.getAbsolutePath() );
		}

		return reportFile;
	}

}
