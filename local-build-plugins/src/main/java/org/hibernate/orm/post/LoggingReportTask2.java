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
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import static org.jboss.jandex.DotName.createSimple;

/**
 * This one would be nice to support, but Jandex is unable to read the annotations
 * from JBoss Logging because it defines those annotations with {@link RetentionPolicy#CLASS};
 * Jandex can only read annotations marked with {@link RetentionPolicy#RUNTIME}
 *
 * @see LoggingReportTask
 *
 * @author Steve Ebersole
 */
public abstract class LoggingReportTask2 extends AbstractJandexAwareTask {
	public static final DotName SUB_SYS_ANN_NAME = createSimple( "org.hibernate.internal.log.SubSystemLogging" );

	public static final DotName MSG_LOGGER_ANN_NAME = createSimple( "org.jboss.logging.annotations.MessageLogger" );
	public static final DotName ID_RANGE_ANN_NAME = createSimple( "org.jboss.logging.annotations.ValidIdRange" );
	public static final DotName MSG_ANN_NAME = createSimple( "org.jboss.logging.annotations.Message" );

	@Inject
	public LoggingReportTask2(IndexManager indexManager, Project project) {
		super(
				indexManager,
				project.getLayout().getBuildDirectory().file( "orm/reports/logging.adoc" )
		);
	}

	@TaskAction
	public void generateLoggingReport() {
		final TreeMap<String, SubSystem> subSystemByName = new TreeMap<>();
		final TreeSet<IdRange> idRanges = new TreeSet<>( Comparator.comparing( IdRange::getMinValue ) );

		final Index index = getIndexManager().getIndex();
		final List<AnnotationInstance> subSysAnnUsages = index.getAnnotations( SUB_SYS_ANN_NAME );
		final List<AnnotationInstance> msgLoggerAnnUsages = index.getAnnotations( MSG_LOGGER_ANN_NAME );

		subSysAnnUsages.forEach( (ann) -> {
			final SubSystem subSystem = new SubSystem(
					ann.value( "name" ).asString(),
					ann.value( "description" ).asString(),
					ann.target().asClass().simpleName()
			);
			subSystemByName.put( subSystem.name, subSystem );
		} );


		msgLoggerAnnUsages.forEach( (msgLoggerAnnUsage) -> {
			// find its id-range annotation, if one
			final ClassInfo loggerClassInfo = msgLoggerAnnUsage.target().asClass();
			final AnnotationInstance subSystemAnnUsage = loggerClassInfo.classAnnotation( SUB_SYS_ANN_NAME );

			final SubSystem subSystem;
			if ( subSystemAnnUsage != null ) {
				subSystem = subSystemByName.get( subSystemAnnUsage.value( "name" ).asString() );
			}
			else {
				subSystem = null;
			}

			final IdRange idRange;
			final AnnotationInstance idRangeAnnUsage = loggerClassInfo.classAnnotation( ID_RANGE_ANN_NAME );
			if ( idRangeAnnUsage == null ) {
				idRange = calculateIdRange( msgLoggerAnnUsage, subSystem );
			}
			else {
				idRange = new IdRange(
						idRangeAnnUsage.value( "min" ).asInt(),
						idRangeAnnUsage.value( "maz" ).asInt(),
						loggerClassInfo.simpleName(),
						true,
						subSystem
				);
				if ( subSystem != null ) {
					subSystem.idRange = idRange;
				}
			}

			if ( idRange != null ) {
				idRanges.add( idRange );
			}
		} );

		generateReport( subSystemByName, idRanges );
	}


	private IdRange calculateIdRange(AnnotationInstance msgLoggerAnnUsage, SubSystem subSystem) {
		final ClassInfo loggerClassInfo = msgLoggerAnnUsage.target().asClass();
		getProject().getLogger().lifecycle( "MessageLogger (`%s`) missing id-range", loggerClassInfo.simpleName() );

		final List<AnnotationInstance> messageAnnUsages = loggerClassInfo.annotations().get( MSG_ANN_NAME );
		if ( messageAnnUsages.isEmpty() ) {
			return null;
		}

		int minId = Integer.MAX_VALUE;
		int maxId = Integer.MIN_VALUE;

		for ( int i = 0; i < messageAnnUsages.size(); i++ ) {
			final AnnotationInstance msgAnnUsage = messageAnnUsages.get( i );
			final int msgId = msgAnnUsage.value( "id" ).asInt();

			if ( msgId < minId ) {
				minId = msgId;
			}
			else if ( msgId > maxId ) {
				maxId = msgId;
			}
		}

		return new IdRange( minId, maxId, loggerClassInfo.simpleName(), false, subSystem );
	}

	private void generateReport(TreeMap<String, SubSystem> subSystemByName, TreeSet<IdRange> idRanges) {
		final File reportFile = prepareReportFile();
		assert reportFile.exists();

		try ( final OutputStreamWriter fileWriter = new OutputStreamWriter( new FileOutputStream( reportFile ) ) ) {
			writeReport( subSystemByName, idRanges, fileWriter );
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Should never happen" );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error writing to report file", e );
		}

	}

	private void writeReport(TreeMap<String, SubSystem> subSystemByName, TreeSet<IdRange> idRanges, OutputStreamWriter fileWriter) {
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
					if ( subSystem.getIdRange() == null ) {
						fileWriter.write( "    * id-range = no\n" );
					}
					else {
						fileWriter.write( "    * id-range = <<" + subSystem.getIdRange().anchorName + ",yes>>\n" );
					}
				}
				catch (IOException e) {
					throw new RuntimeException( "Error writing sub-system entry (" + subSystem.getAnchorName() + ") to report file", e );
				}
			} );

			fileWriter.write( "\n\n" );
			fileWriter.write( "== Keyed message logging\n\n" );
			idRanges.forEach( (idRange) -> {
				try {
					fileWriter.write( "[[" + idRange.getAnchorName() + "]]\n" );
					fileWriter.write( "`" + idRange.getLabel() + "`::\n" );
					fileWriter.write( "    * MessageLogger class = `" + idRange.getLoggerClassName() + "`\n" );
					fileWriter.write( "    * Explicit? = " + idRange.isExplicit() + "\n" );
					final SubSystem subSystem = idRange.getSubSystem();
					if ( subSystem == null ) {
						fileWriter.write( "    * sub-system? = no\n" );
					}
					else {
						fileWriter.write( "    * sub-system? = <<" + subSystem.getAnchorName() + ",yes>>\n" );
					}
				}
				catch (IOException e) {
					throw new RuntimeException( "Error writing msg-id entry (" + idRange.getAnchorName() + ") to report file", e );
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

		private IdRange idRange;

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

		public IdRange getIdRange() {
			return idRange;
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

	private static class IdRange {
		private final int minValue;
		private final int maxValue;
		private final String loggerClassName;
		private final boolean explicit;
		private final SubSystem subSystem;

		private final String anchorName;

		public IdRange(
				int minValue,
				int maxValue,
				String loggerClassName,
				boolean explicit,
				SubSystem subSystem) {
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.loggerClassName = loggerClassName;
			this.explicit = explicit;
			this.subSystem = subSystem;

			this.anchorName = "HHH" + minValue;
		}

		public int getMinValue() {
			return minValue;
		}

		public int getMaxValue() {
			return maxValue;
		}

		public String getLoggerClassName() {
			return loggerClassName;
		}

		public boolean isExplicit() {
			return explicit;
		}

		public SubSystem getSubSystem() {
			return subSystem;
		}

		public String getAnchorName() {
			return anchorName;
		}

		public String getLabel() {
			return String.format( "HHH%06d - HHH%06d", minValue, maxValue );
		}
	}
}
