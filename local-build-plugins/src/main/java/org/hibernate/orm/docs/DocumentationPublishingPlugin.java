/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Plugin for helping with publishing documentation to the doc server - <ul>
 *     <li>Publishes a config extension ({@link DocumentationPublishing}) under {@value #EXT}</li>
 *     <li>Creates a task ({@value #UPLOAD_TASK}) to upload the documentation to the doc server</li>
 *     <li>Creates a task ({@value #GEN_DESC_TASK}) to create the "published doc descriptor" (JSON) file</li>
 *     <li>Creates a task ({@value #UPLOAD_DESC_TASK}) to upload the "published doc descriptor" (JSON) file to the doc server</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class DocumentationPublishingPlugin implements Plugin<Project> {
	public static final String EXT = "documentationPublishing";
	public static final String UPLOAD_TASK = "uploadDocumentation";
	public static final String GEN_DESC_TASK = "generatorDocumentationDescriptor";
	public static final String UPLOAD_DESC_TASK = "uploadDocumentationDescriptor";

	@Override
	public void apply(Project project) {
		final DocumentationPublishing docPubDsl = project.getExtensions().create( EXT, DocumentationPublishing.class );

		final PublishTask uploadTask = project.getTasks().create(
				UPLOAD_TASK,
				PublishTask.class,
				docPubDsl
		);

		final GenerateDescriptorTask generateDescriptorTask = project.getTasks().create(
				GEN_DESC_TASK,
				GenerateDescriptorTask.class,
				docPubDsl
		);

		final PublishDescriptorTask uploadDescriptorTask = project.getTasks().create(
				UPLOAD_DESC_TASK,
				PublishDescriptorTask.class,
				docPubDsl
		);

		final PublishMigrationGuide publishMigrationGuideTask = project.getTasks().create(
				PublishMigrationGuide.NAME,
				PublishMigrationGuide.class,
				docPubDsl
		);
		publishMigrationGuideTask.getMigrationGuideDirectory().convention( project.getLayout().getBuildDirectory().dir( "asciidoc/migration-guide" ) );

		// todo - incorporate HibernateVersion from `gradle/base-information.gradle`
		final boolean isSnapshot = project.getVersion().toString().endsWith( "-SNAPSHOT" );
		uploadTask.onlyIf( (task) -> !isSnapshot );
		uploadDescriptorTask.onlyIf( (task) -> !isSnapshot && generateDescriptorTask.getDidWork() );

		uploadTask.dependsOn( uploadDescriptorTask );
		uploadDescriptorTask.dependsOn( generateDescriptorTask );
	}
}
