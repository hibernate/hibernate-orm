/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import static org.hibernate.orm.docs.DocumentationPublishing.DSL_NAME;
import static org.hibernate.orm.docs.GenerateDescriptorTask.GEN_DESC_TASK_NAME;
import static org.hibernate.orm.docs.PublishDescriptorTask.UPLOAD_DESC_TASK_NAME;
import static org.hibernate.orm.docs.PublishTask.UPLOAD_TASK_NAME;

/**
 * Plugin for helping with publishing documentation to the doc server - <ul>
 *     <li>Publishes a config extension ({@link DocumentationPublishing}) under {@value DocumentationPublishing#DSL_NAME}</li>
 *     <li>Creates a task ({@value PublishTask#UPLOAD_TASK_NAME}) to upload the documentation to the doc server</li>
 *     <li>Creates a task ({@value GenerateDescriptorTask#GEN_DESC_TASK_NAME}) to create the "published doc descriptor" (JSON) file</li>
 *     <li>Creates a task ({@value PublishDescriptorTask#UPLOAD_DESC_TASK_NAME}) to upload the "published doc descriptor" (JSON) file to the doc server</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class DocumentationPublishingPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		final DocumentationPublishing docPubDsl = project.getExtensions().create( DSL_NAME, DocumentationPublishing.class );

		final boolean isSnapshot = project.getVersion().toString().endsWith( "-SNAPSHOT" );

		final TaskProvider<GenerateDescriptorTask> generateDescriptorTask = project.getTasks().register(
				GEN_DESC_TASK_NAME,
				GenerateDescriptorTask.class,
				(task) -> {
					task.getCurrentlyBuildingFamily().convention( docPubDsl.getReleaseFamilyIdentifier() );
					task.getJsonFile().convention( docPubDsl.getUpdatedJsonFile() );
				}
		);

		final TaskProvider<PublishDescriptorTask> uploadDescriptorTask = project.getTasks().register(
				UPLOAD_DESC_TASK_NAME,
				PublishDescriptorTask.class,
				(task) -> {
					task.getDocDescriptorUploadUrl().convention( docPubDsl.getDocDescriptorUploadUrl() );
					task.getJsonFile().convention( docPubDsl.getUpdatedJsonFile() );

					task.dependsOn( generateDescriptorTask );
					task.onlyIf( (t) -> !isSnapshot && generateDescriptorTask.get().getDidWork() );
				}
		);

		//noinspection unused
		final TaskProvider<PublishMigrationGuide> publishMigrationGuideTask = project.getTasks().register(
				PublishMigrationGuide.NAME,
				PublishMigrationGuide.class,
				(task) -> {
					task.getCurrentlyBuildingFamily().convention( docPubDsl.getReleaseFamilyIdentifier() );
					task.getDocServerUrl().convention( docPubDsl.getDocServerUrl() );
					task.getMigrationGuideDirectory().convention( project.getLayout().getBuildDirectory().dir( "documentation/migration-guide" ) );
				}
		);

		//noinspection unused
		final TaskProvider<PublishTask> uploadTask = project.getTasks().register(
				UPLOAD_TASK_NAME,
				PublishTask.class,
				(task) -> {
					task.getBuildingFamily().convention( docPubDsl.getReleaseFamilyIdentifier() );
					task.getDocServerUrl().convention( docPubDsl.getDocServerUrl() );
					task.getStagingDirectory().convention( docPubDsl.getStagingDirectory() );

					task.dependsOn( uploadDescriptorTask );
					task.onlyIf( (t) -> !isSnapshot );
				}
		);
	}
}
