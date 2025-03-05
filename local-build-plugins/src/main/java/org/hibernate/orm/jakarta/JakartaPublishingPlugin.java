/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta;

import java.util.function.Consumer;
import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.DocsType;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * Models a publishable Jakartafied project
 *
 * @author Steve Ebersole
 */
public class JakartaPublishingPlugin implements Plugin<Project> {
	public static final String MAIN_CONFIG_NAME = "jakartaElements";
	public static final String MAIN_JAR_TASK_NAME = "jakartafyJar";

	public static final String SOURCES_CONFIG_NAME = "jakartaSourcesElements";
	public static final String SOURCES_JAR_TASK_NAME = "jakartafySourcesJar";

	public static final String JAVADOC_CONFIG_NAME = "jakartaJavadocElements";
	public static final String JAVADOC_JAR_TASK_NAME = "jakartafyJavadocJar";

	private final SoftwareComponentFactory softwareComponentFactory;

	@Inject
	public JakartaPublishingPlugin(SoftwareComponentFactory softwareComponentFactory) {
		this.softwareComponentFactory = softwareComponentFactory;
	}

	@Override
	public void apply(Project project) {
		project.getPlugins().apply( JakartaPlugin.class );

		final AdhocComponentWithVariants jakartaComponent = softwareComponentFactory.adhoc( "jakarta" );
		project.getComponents().add( jakartaComponent );

		addMainVariant( jakartaComponent, project );
		addSourcesVariant( jakartaComponent, project );
		addJavadocVariant( jakartaComponent, project );
	}

	private void addMainVariant(
			AdhocComponentWithVariants jakartaComponent,
			Project project) {
		final ObjectFactory objectFactory = project.getObjects();
		final Task jakartafyJar = project.getTasks().getByName( "jakartafyJar" );

		addVariant(
				MAIN_CONFIG_NAME,
				MAIN_JAR_TASK_NAME,
				null,
				jakartaComponent,
				jakartafyJar,
				(attributes) -> {
					attributes.attribute(
							Usage.USAGE_ATTRIBUTE,
							objectFactory.named( Usage.class, Usage.JAVA_RUNTIME )
					);

					attributes.attribute(
							Category.CATEGORY_ATTRIBUTE,
							objectFactory.named( Category.class, Category.LIBRARY )
					);

					attributes.attribute(
							Bundling.BUNDLING_ATTRIBUTE,
							objectFactory.named( Bundling.class, Bundling.EXTERNAL )
					);

					attributes.attribute(
							LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
							objectFactory.named( LibraryElements.class, LibraryElements.CLASSES_AND_RESOURCES )
					);
				},
				project
		);
	}

	private void addSourcesVariant(AdhocComponentWithVariants jakartaComponent, Project project) {
		final ObjectFactory objectFactory = project.getObjects();
		final Task jakartafyJar = project.getTasks().getByName( "jakartafySourcesJar" );

		addVariant(
				SOURCES_CONFIG_NAME,
				SOURCES_JAR_TASK_NAME,
				"sources",
				jakartaComponent,
				jakartafyJar,
				(attributes) -> {
					attributes.attribute(
							Usage.USAGE_ATTRIBUTE,
							objectFactory.named( Usage.class, Usage.JAVA_RUNTIME )
					);

					attributes.attribute(
							Bundling.BUNDLING_ATTRIBUTE,
							objectFactory.named( Bundling.class, Bundling.EXTERNAL )
					);

					attributes.attribute(
							Category.CATEGORY_ATTRIBUTE,
							objectFactory.named( Category.class, Category.DOCUMENTATION )
					);

					attributes.attribute(
							DocsType.DOCS_TYPE_ATTRIBUTE,
							objectFactory.named( DocsType.class, DocsType.SOURCES )
					);
				},
				project
		);
	}

	private void addJavadocVariant(AdhocComponentWithVariants jakartaComponent, Project project) {
		final ObjectFactory objectFactory = project.getObjects();
		final Task jakartafyJar = project.getTasks().getByName( "jakartafyJavadocJar" );

		addVariant(
				JAVADOC_CONFIG_NAME,
				JAVADOC_JAR_TASK_NAME,
				"javadoc",
				jakartaComponent,
				jakartafyJar,
				(attributes) -> {
					attributes.attribute(
							Usage.USAGE_ATTRIBUTE,
							objectFactory.named( Usage.class, Usage.JAVA_RUNTIME )
					);

					attributes.attribute(
							Bundling.BUNDLING_ATTRIBUTE,
							objectFactory.named( Bundling.class, Bundling.EXTERNAL )
					);

					attributes.attribute(
							Category.CATEGORY_ATTRIBUTE,
							objectFactory.named( Category.class, Category.DOCUMENTATION )
					);

					attributes.attribute(
							DocsType.DOCS_TYPE_ATTRIBUTE,
							objectFactory.named( DocsType.class, DocsType.JAVADOC )
					);
				},
				project
		);
	}


	private void addVariant(
			String configName,
			String jarTaskName,
			String classifier,
			AdhocComponentWithVariants jakartaComponent,
			Task jakartaJarTask,
			Consumer<AttributeContainer> attributesAdjuster,
			Project project) {
		final Configuration variantConfig = project.getConfigurations().create(
				configName,
				(publicationConfiguration) -> {
					publicationConfiguration.setDescription( "Consumable configuration for Jakartafied sources jar" );
					publicationConfiguration.setCanBeConsumed( true );
					publicationConfiguration.setCanBeResolved( false );
					publicationConfiguration.setVisible( false );

					attributesAdjuster.accept( publicationConfiguration.getAttributes() );
				}
		);

		jakartaComponent.addVariantsFromConfiguration(
				variantConfig,
				(variantDetails) -> variantDetails.mapToMavenScope( "runtime" )
		);

		final TaskProvider<JakartaJarTransformation> mainJarTask;
		final TaskContainer tasks = project.getTasks();
		if ( ! tasks.getNames().contains( jarTaskName ) ) {
			mainJarTask = tasks.register(
					jarTaskName,
					JakartaJarTransformation.class,
					(jakartaficationTask) -> jakartaficationTask.setDescription( "Produces the Jakartafied main jar for `" + project.getPath() + "`" )
			);
			if ( tasks.getNames().contains( "assemble" ) ) {
				tasks.named( "assemble" ).configure( (assembleTask) -> assembleTask.dependsOn( mainJarTask ) );
			}
		}
		else {
			mainJarTask = tasks.named( jarTaskName, JakartaJarTransformation.class );
		}

		variantConfig.getOutgoing().artifact(
				mainJarTask.get().getTargetJar(),
				(artifact) -> {
					artifact.setClassifier( classifier );
					artifact.setExtension( "jar" );
					artifact.builtBy( jakartaJarTask );
				}
		);
	}

}
