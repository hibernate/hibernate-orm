/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.jakarta;

import java.io.File;
import java.util.Set;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import groovy.lang.Closure;

import static org.hibernate.orm.jakarta.JakartaPlugin.JAKARTA;

/**
 * @author Steve Ebersole
 */
@CacheableTask
public abstract class JakartaDependencyTransformation extends DefaultTask {
	private final Property<Object> dependency;
	private final RegularFileProperty targetFile;

	@Inject
	public JakartaDependencyTransformation(ObjectFactory objectFactory) {
		dependency = objectFactory.property( Object.class );
		targetFile = objectFactory.fileProperty();

		setGroup( JAKARTA );
	}

	@Input
	public Property<Object> getDependency() {
		return dependency;
	}

	@OutputFile
	public RegularFileProperty getTargetFile() {
		return targetFile;
	}

	@TaskAction
	void transform() {
		// first, we need to resolve the dependency...
		final Dependency dependency = getProject().getDependencies().create( this.dependency.get() );
		final Set<ResolvedDependency> firstLevelDependencies = getProject().getConfigurations()
				.detachedConfiguration( dependency )
				.getResolvedConfiguration()
				.getFirstLevelModuleDependencies();

		if ( firstLevelDependencies.isEmpty() ) {
			throw new RuntimeException( "Given dependency [" + this.dependency + "] resulted in no resolved dependencies" );
		}
		if ( firstLevelDependencies.size() > 1 ) {
			throw new RuntimeException( "Given dependency [" + this.dependency + "] resulted in more than one resolved dependencies" );
		}

		final ResolvedDependency resolvedDependency = firstLevelDependencies.iterator().next();
		final Set<ResolvedArtifact> resolvedDependencyArtifacts = resolvedDependency.getModuleArtifacts();

		if ( resolvedDependencyArtifacts.isEmpty() ) {
			throw new RuntimeException( "Given dependency [" + this.dependency + "] resulted in no resolved artifacts" );
		}
		if ( resolvedDependencyArtifacts.size() > 1 ) {
			throw new RuntimeException( "Given dependency [" + this.dependency + "] resulted in more than one resolved artifacts" );
		}

		final ResolvedArtifact resolvedArtifact = resolvedDependencyArtifacts.iterator().next();

		// this is the file we want to
		final File sourceFile = resolvedArtifact.getFile();

		// and this is the file to which we want to write the transformation...
		final File targetDirAsFile = targetFile.get().getAsFile();

		getProject().javaexec(
				(javaExecSpec) -> {
					javaExecSpec.classpath( getProject().getConfigurations().getByName( "jakartaeeTransformTool" ) );
					javaExecSpec.setMain( "org.eclipse.transformer.jakarta.JakartaTransformer" );
					javaExecSpec.args(
							sourceFile.getAbsolutePath(),
							targetDirAsFile.getAbsolutePath(),
							"-q",
							"-tr", getProject().getRootProject().file( "rules/jakarta-renames.properties" ).getAbsolutePath(),
							"-tv", getProject().getRootProject().file( "rules/jakarta-versions.properties" ).getAbsolutePath(),
							"-td", getProject().getRootProject().file( "rules/jakarta-direct.properties" ).getAbsolutePath()
					);
				}
		);

		getProject().javaexec(
				(javaExecSpec) -> {
					javaExecSpec.classpath( getProject().getConfigurations().getByName( "jakartaeeTransformTool" ) );
					javaExecSpec.setMain( "org.eclipse.transformer.jakarta.JakartaTransformer" );
					javaExecSpec.args(
							sourceFile.getAbsolutePath(),
							targetDirAsFile.getAbsolutePath(),
							"-q",
							"-tr", getProject().getRootProject().file( "rules/jakarta-renames.properties" ).getAbsolutePath(),
							"-tv", getProject().getRootProject().file( "rules/jakarta-versions.properties" ).getAbsolutePath(),
							"-td", getProject().getRootProject().file( "rules/jakarta-direct.properties" ).getAbsolutePath()
					);
				}
		);
	}
}
