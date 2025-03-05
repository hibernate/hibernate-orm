/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.docs;

import java.util.Locale;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import static org.hibernate.orm.docs.DocumentationPublishing.DSL_NAME;
import static org.hibernate.orm.docs.GenerateDescriptorTask.GEN_DESC_TASK_NAME;
import static org.hibernate.orm.docs.PublishDescriptorTask.UPLOAD_DESC_TASK_NAME;
import static org.hibernate.orm.docs.PublishTask.UPLOAD_TASK_NAME;

/**
 * Plugin for helping with publishing documentation to the doc server - <ul>
 *     <li>Publishes a {@link DocumentationPublishing DSL extension} under {@value DocumentationPublishing#DSL_NAME}</li>
 *     <li>Creates a task to upload the documentation to the doc server - {@value PublishTask#UPLOAD_TASK_NAME}</li>
 *     <li>Creates a task to create the doc descriptor (JSON) file - {@value GenerateDescriptorTask#GEN_DESC_TASK_NAME}</li>
 *     <li>Creates a task to upload the doc descriptor (JSON) file to the doc server - {@value PublishDescriptorTask#UPLOAD_DESC_TASK_NAME}</li>
 *     <li>Creates a task to update symlinks on the doc server - {@value UpdateSymLinksTask#SYMLINKS_TASK_NAME}</li>
 *     <li>Creates a task to upload the migration guide to the doc server - {@value PublishMigrationGuide#NAME}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class DocumentationPublishingPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		final DocumentationPublishing docPubDsl = project.getExtensions().create( DSL_NAME, DocumentationPublishing.class );

		final boolean isSnapshot = project.getVersion().toString().endsWith( "-SNAPSHOT" );
		final boolean isFinal = project.getVersion().toString().endsWith( ".Final" );

		final TaskProvider<GenerateDescriptorTask> generateDescriptorTask = project.getTasks().register(
				GEN_DESC_TASK_NAME,
				GenerateDescriptorTask.class,
				(task) -> {
					task.getCurrentlyBuildingFamily().convention( docPubDsl.getReleaseFamilyIdentifier() );
					task.getJsonFile().convention( docPubDsl.getUpdatedJsonFile() );

					task.onlyIf( (t) -> isFinal );
				}
		);

		final TaskProvider<PublishDescriptorTask> uploadDescriptorTask = project.getTasks().register(
				UPLOAD_DESC_TASK_NAME,
				PublishDescriptorTask.class,
				(task) -> {
					task.getDocDescriptorUploadUrl().convention( defaultDescriptorUploadUrl( docPubDsl ) );
					task.getJsonFile().convention( docPubDsl.getUpdatedJsonFile() );

					task.dependsOn( generateDescriptorTask );
					task.onlyIf( (t) -> generateDescriptorTask.get().getDidWork() && generateDescriptorTask.get().needsUpload() );
				}
		);

		//noinspection unused
		final TaskProvider<PublishTask> uploadTask = project.getTasks().register(
				UPLOAD_TASK_NAME,
				PublishTask.class,
				(task) -> {
					task.getBuildingFamily().convention( docPubDsl.getReleaseFamilyIdentifier() );
					task.getStagingDirectory().convention( docPubDsl.getStagingDirectory() );
					task.getDocServerUrl().convention( defaultDocUploadUrl( docPubDsl ) );

					task.dependsOn( uploadDescriptorTask );
					task.onlyIf( (t) -> !isSnapshot );
				}
		);

		//noinspection unused
		final TaskProvider<UpdateSymLinksTask> symLinkTask = project.getTasks().register(
				UpdateSymLinksTask.SYMLINKS_TASK_NAME,
				UpdateSymLinksTask.class,
				(task) -> {
					task.getBuildingFamily().convention( docPubDsl.getReleaseFamilyIdentifier() );
					task.getSftpDocServer().convention( docPubDsl.getSftpDocServer() );
					task.getServerBaseDir().convention( docPubDsl.getServerBaseDir() );

					task.dependsOn( generateDescriptorTask );
					task.dependsOn( uploadTask );
					task.onlyIf( (t) -> generateDescriptorTask.get().getDidWork() && generateDescriptorTask.get().needsSymLinkUpdate() );
				}
		);

		//noinspection unused
		final TaskProvider<PublishMigrationGuide> publishMigrationGuideTask = project.getTasks().register(
				PublishMigrationGuide.NAME,
				PublishMigrationGuide.class,
				(task) -> {
					task.getCurrentlyBuildingFamily().convention( docPubDsl.getReleaseFamilyIdentifier() );
					task.getDocServerUrl().convention( defaultDocUploadUrl( docPubDsl ) );
					task.getMigrationGuideDirectory().convention( project.getLayout().getBuildDirectory().dir( "documentation/migration-guide" ) );
				}
		);
	}

	private Provider<String> defaultDescriptorUploadUrl(DocumentationPublishing dsl) {
		return dsl.getRsyncDocServer()
				.map( (server) -> String.format( Locale.ROOT, "%s:%s/_outdated-content/orm.json", server, dsl.getServerBaseDir().get() ) );
	}

	private Provider<String> defaultDocUploadUrl(DocumentationPublishing dsl) {
		return dsl.getRsyncDocServer()
				.map( (server) -> String.format( Locale.ROOT, "%s:%s/orm", server, dsl.getServerBaseDir().get() ) );
	}
}
