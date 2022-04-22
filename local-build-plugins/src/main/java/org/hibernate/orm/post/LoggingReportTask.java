/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.TreeMap;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import static org.jboss.jandex.DotName.createSimple;

/**
 * @author Steve Ebersole
 */
public abstract class LoggingReportTask extends AbstractJandexAwareTask {
	public static final DotName SUB_SYS_ANN_NAME = createSimple( "org.hibernate.internal.log.SubSystemLogging" );

	@Inject
	public LoggingReportTask(IndexManager indexManager, Project project) {
		super(
				indexManager,
				project.getLayout().getBuildDirectory().file( "orm/reports/logging.adoc" )
		);
	}

	@TaskAction
	public void generateLoggingReport() {
		final TreeMap<String, SubSystem> subSystemByName = new TreeMap<>();

		final Index index = getIndexManager().getIndex();
		final List<AnnotationInstance> subSysAnnUsages = index.getAnnotations( SUB_SYS_ANN_NAME );

		subSysAnnUsages.forEach( (ann) -> {
			final SubSystem subSystem = new SubSystem(
					ann.value( "name" ).asString(),
					ann.value( "description" ).asString(),
					ann.target().asClass().simpleName()
			);
			subSystemByName.put( subSystem.name, subSystem );
		} );

		generateReport( subSystemByName );
	}


	private void generateReport(TreeMap<String, SubSystem> subSystemByName) {
		final File reportFile = prepareReportFile();
		assert reportFile.exists();

		try ( final OutputStreamWriter fileWriter = new OutputStreamWriter( new FileOutputStream( reportFile ) ) ) {
			writeReport( subSystemByName, fileWriter );
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Should never happen" );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error writing to report file", e );
		}

	}

	private void writeReport(TreeMap<String, SubSystem> subSystemByName, OutputStreamWriter fileWriter) {
		// for the moment, just dump these to a file in simple text format.
		//
		// ultimately come back and create an asciidoctor-formatted file
		// and run through the asciidoctor task

		try {
			fileWriter.write( "= Hibernate logging\n\n" );

			fileWriter.write( "== Sub-system logging\n\n" );
			subSystemByName.forEach( (name, subSystem) -> {
				try {
					fileWriter.write( "[[" + subSystem.getAnchorName() + "]]\n" );
					fileWriter.write( "`" + subSystem.getName() + "`::\n" );
					fileWriter.write( "    * Logging class-name = `" + subSystem.getLoggingClassName() + "`\n" );
					fileWriter.write( "    * Description = " + subSystem.getDescription() + "\n" );
				}
				catch (IOException e) {
					throw new RuntimeException( "Error writing sub-system entry (" + subSystem.getAnchorName() + ") to report file", e );
				}
			} );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error writing to report file", e );
		}
	}


	private static class SubSystem {
		private final String name;
		private final String description;
		private final String loggingClassName;

		private final String anchorName;

		public SubSystem(String name, String description, String loggingClassName) {
			this.name = name;
			this.description = description;
			this.loggingClassName = loggingClassName;

			this.anchorName = determineAnchorName( name );
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public String getLoggingClassName() {
			return loggingClassName;
		}

		public String getAnchorName() {
			return anchorName;
		}

		private static String determineAnchorName(final String name) {
			final String baseName;
			if ( name.startsWith( "org.hibernate.orm." ) ) {
				baseName = name.substring( "org.hibernate.orm.".length() );
			}
			else if ( name.startsWith( "org.hibernate." ) ) {
				baseName = name.substring( "org.hibernate.".length() );
			}
			else {
				baseName = name;
			}

			return baseName.replace( '.', '_' );
		}
	}

}
