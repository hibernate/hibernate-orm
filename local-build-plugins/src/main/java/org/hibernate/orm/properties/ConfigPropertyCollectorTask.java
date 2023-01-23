/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.hibernate.orm.properties.processor.ConfigPropertyHolder;
import org.hibernate.orm.properties.processor.Configuration;
import org.hibernate.orm.properties.processor.ConfigurationPropertyProcessor;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

/**
 * Task that goes to the root project and then sends all the sources from children projects trough annotation processing
 * collecting all the config properties into a map. See {@link ConfigurationPropertyProcessor}
 */
public class ConfigPropertyCollectorTask extends DefaultTask {

	private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private final Project project;
	private final ConfigPropertyHolder properties;
	private final Path target;

	@Inject
	public ConfigPropertyCollectorTask(ConfigPropertyHolder properties, Project project) {
		this.project = project;
		this.properties = properties;
		this.target = project.getBuildDir().toPath();
	}


	@TaskAction
	public void collectProperties() {
		for ( Map.Entry<String, Project> projectEntry : project.getRootProject().getChildProjects().entrySet() ) {
			try {
				// we don't need to look at testing projects as these aren't for public configurations.
				if ( projectEntry.getKey().contains( "test" ) ) {
					continue;
				}
				SourceSetContainer sources = projectEntry.getValue().getExtensions().getByType(
						SourceSetContainer.class );

				sources.all( s -> {
					// no need to compile/process test sources:
					if ( !"test".equals( s.getName() ) ) {
						compile(
								projectEntry.getValue(),
								s.getAllJava().getSourceDirectories().getFiles(),
								s.getCompileClasspath().getFiles()
						);
					}
				} );
			}
			catch (UnknownDomainObjectException e) {
				getLogger().info( "Ignoring " + projectEntry.getKey() + " because of " + e.getMessage(), e );
			}
		}
	}

	public boolean compile(Project project, Collection<File> sources, Collection<File> classpath) {
		List<File> classes = new ArrayList<>();
		for ( File sourceFile : sources ) {
			try {
				// need to find all java files to be later converted to compilation units:
				Files.walkFileTree( sourceFile.toPath(), new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if ( file.getFileName().toString().endsWith( ".java" ) ) {
							classes.add( file.toFile() );
						}
						return FileVisitResult.CONTINUE;
					}
				} );
			}
			catch (IOException e) {
				getLogger().debug( "Failed to process " + sourceFile.getAbsolutePath(), e );
			}
		}

		if ( classes.isEmpty() ) {
			return true;
		}

		StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(
				classes.stream().toArray( File[]::new ) );

		try {
			// we don't really need the compiled classes, so we just dump them somewhere to not mess with the rest of the
			// classes:
			fileManager.setLocation(
					StandardLocation.CLASS_OUTPUT,
					Arrays.asList(
							Files.createDirectories( target.resolve( "config-property-collector-compiled-classes" ) )
									.toFile() )
			);
			fileManager.setLocation(
					StandardLocation.CLASS_PATH,
					classpath
			);
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}

		List<String> options = new ArrayList<>();
		options.add(
				String.format(
						Locale.ROOT,
						"-A%s=%s",
						Configuration.MODULE_TITLE,
						project.getDescription()
				)
		);
		options.add(
				String.format(
						Locale.ROOT,
						"-A%s=%s",
						Configuration.MODULE_LINK_ANCHOR,
						project.getName() + "-"
				)
		);
		// todo: this should come from some plugin/task config rather than be hardcoded:
		options.add(
				String.format(
						Locale.ROOT,
						"-A%s=%s",
						Configuration.JAVADOC_LINK,
						"https://docs.jboss.org/hibernate/orm/6.2/javadocs/"
				)
		);

		JavaCompiler.CompilationTask task = compiler.getTask(
				null,
				fileManager,
				null,
				options,
				null,
				compilationUnits
		);

		task.setProcessors( Arrays.asList( new ConfigurationPropertyProcessor(
				project.getBuildDir().toPath().resolve( "docs/javadoc" ),
				properties
		) ) );

		return task.call();
	}


}
