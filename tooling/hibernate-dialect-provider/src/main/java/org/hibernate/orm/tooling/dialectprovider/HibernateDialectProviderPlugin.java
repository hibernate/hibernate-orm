/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider;

import java.io.File;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;
import org.hibernate.orm.tooling.dialectprovider.internal.HibernateVersions;
import org.hibernate.orm.tooling.dialectprovider.internal.PluginVersions;
import org.hibernate.orm.tooling.dialectprovider.internal.ValidateProviderTestRuntime;

/// Adds static provider-boundary validation and database-free Dialect contract
/// testing to an external Java project.
///
/// @author Steve Ebersole
/// @since 8.0
public final class HibernateDialectProviderPlugin implements Plugin<Project> {
	public static final String EXTENSION_NAME = "hibernateDialectProvider";
	public static final String VERIFICATION_TASK_NAME = "verifyDialectProvider";

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply( JavaPlugin.class );
		final JavaPluginExtension java = project.getExtensions().getByType( JavaPluginExtension.class );
		final SourceSet main = java.getSourceSets().getByName( SourceSet.MAIN_SOURCE_SET_NAME );
		final Configuration compileClasspath = project.getConfigurations()
				.getByName( main.getCompileClasspathConfigurationName() );
		final Configuration runtimeClasspath = project.getConfigurations()
				.getByName( main.getRuntimeClasspathConfigurationName() );
		final HibernateDialectProviderExtension extension = project.getExtensions().create(
				EXTENSION_NAME,
				HibernateDialectProviderExtension.class
		);
		final org.gradle.api.provider.Provider<String> resolvedCoreVersion = project.provider(
				() -> HibernateVersions.resolve( compileClasspath, runtimeClasspath )
		);
		extension.getProviderPackages().convention( List.of() );
		extension.getContractProfiles().convention( List.of() );
		extension.getHibernateVersion().convention( resolvedCoreVersion );
		extension.getClassificationMetadataBaseUrl().convention( "https://docs.hibernate.org/orm" );
		extension.getAttachToCheck().convention( true );
		extension.getWarningsAsErrors().convention( false );

		final TaskProvider<Jar> jar = project.getTasks().named( JavaPlugin.JAR_TASK_NAME, Jar.class );
		extension.getProviderArtifacts().from( jar.flatMap( Jar::getArchiveFile ) );
		extension.getHibernateArtifacts().from( hibernateArtifacts( compileClasspath ), hibernateArtifacts( runtimeClasspath ) );

		final TaskProvider<ResolveDialectProviderClassificationMetadata> resolveMetadata = project.getTasks().register(
				"resolveDialectProviderClassificationMetadata",
				ResolveDialectProviderClassificationMetadata.class,
				task -> {
					task.setGroup( "verification" );
					task.setDescription( "Resolves verified Hibernate ORM classification metadata for this provider" );
					task.getHibernateVersion().set( extension.getHibernateVersion() );
					task.getResolvedCoreVersion().set( resolvedCoreVersion );
					task.getPluginVersion().set( PluginVersions.hibernateOrm() );
					task.getClassificationMetadataBaseUrl().set( extension.getClassificationMetadataBaseUrl() );
					task.getClassificationMetadataFile().set( extension.getClassificationMetadataFile() );
					task.getOffline().set( project.getGradle().getStartParameter().isOffline() );
					task.getRefreshDependencies().set( project.getGradle().getStartParameter().isRefreshDependencies() );
					task.getSharedCacheDirectory().set(
						project.getLayout().dir( project.provider( () -> new File(
								project.getGradle().getGradleUserHomeDir(),
								"caches/hibernate-orm/dialect-provider-metadata"
						) ) )
				);
					task.getResolvedMetadataFile().convention(
							project.getLayout().getBuildDirectory()
									.file( "hibernate-dialect-provider/metadata/classifications.json.gz" )
					);
				}
		);

		final TaskProvider<ValidateDialectProviderBoundaries> validate = project.getTasks().register(
				"validateDialectProviderBoundaries",
				ValidateDialectProviderBoundaries.class,
				task -> {
					task.setGroup( "verification" );
					task.setDescription( "Validates provider bytecode against Hibernate ORM API/SPI boundaries" );
					task.dependsOn( jar, resolveMetadata );
					task.getProviderArtifacts().from( extension.getProviderArtifacts() );
					task.getHibernateArtifacts().from( extension.getHibernateArtifacts() );
					task.getProviderPackages().set( extension.getProviderPackages() );
					task.getWarningsAsErrors().set( extension.getWarningsAsErrors() );
					task.getClassificationMetadataFile().set( resolveMetadata.flatMap(
						ResolveDialectProviderClassificationMetadata::getResolvedMetadataFile
				) );
					task.getTextReportFile().convention(
						project.getLayout().getBuildDirectory()
								.file( "reports/hibernate-dialect-provider/boundary-validation.txt" )
				);
					task.getJsonReportFile().convention(
						project.getLayout().getBuildDirectory()
								.file( "reports/hibernate-dialect-provider/boundary-validation.json" )
				);
				}
		);

		final SourceSet providerTest = java.getSourceSets().create( "dialectProviderTest" );
		final TaskProvider<GenerateDialectProviderContractTests> generate = project.getTasks().register(
				"generateDialectProviderContractTests",
				GenerateDialectProviderContractTests.class,
				task -> {
					task.setGroup( "verification" );
					task.setDescription( "Generates JUnit bridges for configured Dialect contract profiles" );
					task.getContractProfiles().set( extension.getContractProfiles() );
					task.getOutputDirectory().convention(
						project.getLayout().getBuildDirectory().dir( "generated/sources/dialectProviderTest/java" )
				);
				}
		);
		providerTest.getJava().srcDir( generate.flatMap( GenerateDialectProviderContractTests::getOutputDirectory ) );
		providerTest.setCompileClasspath( providerTest.getCompileClasspath().plus( main.getOutput() ) );
		providerTest.setRuntimeClasspath( providerTest.getRuntimeClasspath().plus( main.getOutput() ) );
		configureProviderTestDependencies( project, main, providerTest, extension );
		final Configuration providerTestRuntimeClasspath = project.getConfigurations()
				.getByName( providerTest.getRuntimeClasspathConfigurationName() );
		final TaskProvider<ValidateProviderTestRuntime> validateTestRuntime = project.getTasks().register(
				"validateDialectProviderTestRuntime",
				ValidateProviderTestRuntime.class,
				task -> {
					task.setGroup( "verification" );
					task.setDescription( "Validates exact Hibernate Core and Dialect test-kit alignment" );
					task.getConfiguredCoreVersion().set( extension.getHibernateVersion() );
					task.getResolvedCoreVersion().set( resolvedCoreVersion );
					task.getPluginVersion().set( PluginVersions.hibernateOrm() );
					task.getContractProfiles().set( extension.getContractProfiles() );
					task.getResolvedTestKitVersions().set( project.provider(
						() -> providerTestRuntimeClasspath.getIncoming().getResolutionResult().getAllComponents().stream()
								.map( component -> component.getId() )
								.filter( ModuleComponentIdentifier.class::isInstance )
								.map( ModuleComponentIdentifier.class::cast )
								.filter( module -> "org.hibernate.orm".equals( module.getGroup() )
										&& "hibernate-dialect-testkit".equals( module.getModule() ) )
								.map( ModuleComponentIdentifier::getVersion )
								.sorted()
								.toList()
				) );
				}
		);

		final TaskProvider<Test> providerTestTask = project.getTasks().register(
				"dialectProviderTest",
				Test.class,
				task -> {
					task.setGroup( "verification" );
					task.setDescription( "Runs generated and provider-authored Dialect contract tests" );
					task.setTestClassesDirs( providerTest.getOutput().getClassesDirs() );
					task.setClasspath( providerTest.getRuntimeClasspath() );
					task.dependsOn( validateTestRuntime );
					task.useJUnitPlatform();
				}
		);

		final TaskProvider<org.gradle.api.Task> verify = project.getTasks().register(
				VERIFICATION_TASK_NAME,
				task -> {
					task.setGroup( "verification" );
					task.setDescription( "Validates and contract-tests the Hibernate ORM Dialect provider" );
					task.dependsOn( validate, providerTestTask );
				}
		);
		project.getTasks().named( LifecycleBasePlugin.CHECK_TASK_NAME ).configure(
				task -> task.dependsOn( project.provider(
						() -> extension.getAttachToCheck().get() ? List.of( verify.get() ) : List.of()
				) )
		);
	}

	private static FileCollection hibernateArtifacts(Configuration configuration) {
		return configuration.getIncoming().artifactView( view -> view.componentFilter(
				identifier -> identifier instanceof ModuleComponentIdentifier module
						&& "org.hibernate.orm".equals( module.getGroup() )
		) ).getFiles();
	}

	private static void configureProviderTestDependencies(
			Project project,
			SourceSet main,
			SourceSet providerTest,
			HibernateDialectProviderExtension extension) {
		project.getConfigurations().getByName( providerTest.getImplementationConfigurationName() )
				.extendsFrom( project.getConfigurations().getByName( main.getImplementationConfigurationName() ) );
		project.getConfigurations().getByName( providerTest.getCompileOnlyConfigurationName() )
				.extendsFrom( project.getConfigurations().getByName( main.getCompileOnlyConfigurationName() ) );
		project.getConfigurations().getByName( providerTest.getRuntimeOnlyConfigurationName() )
				.extendsFrom( project.getConfigurations().getByName( main.getRuntimeOnlyConfigurationName() ) );

		final Configuration implementation = project.getConfigurations()
				.getByName( providerTest.getImplementationConfigurationName() );
		implementation.withDependencies( dependencies -> {
			dependencies.add( project.getDependencies().create(
					"org.junit.jupiter:junit-jupiter-api:" + PluginVersions.junitJupiter()
			) );
			if ( !extension.getContractProfiles().get().isEmpty() ) {
				dependencies.add( project.getDependencies().create(
						"org.hibernate.orm:hibernate-dialect-testkit:" + extension.getHibernateVersion().get()
				) );
			}
		} );
		project.getConfigurations().getByName( providerTest.getRuntimeOnlyConfigurationName() )
				.withDependencies( dependencies -> {
					dependencies.add( project.getDependencies().create(
							"org.junit.jupiter:junit-jupiter-engine:" + PluginVersions.junitJupiter()
					) );
					dependencies.add( project.getDependencies().create(
							"org.junit.platform:junit-platform-launcher:" + PluginVersions.junitPlatform()
					) );
				} );
	}
}
