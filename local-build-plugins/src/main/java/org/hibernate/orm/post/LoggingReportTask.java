/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import javax.inject.Inject;
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
public abstract class LoggingReportTask extends AbstractJandexAwareTask {
	public static final DotName SUB_SYS_ANN_NAME = createSimple( "org.hibernate.internal.log.SubSystemLogging" );

	public static final DotName MSG_LOGGER_ANN_NAME = createSimple( "org.jboss.logging.annotations.MessageLogger" );
	public static final DotName ID_RANGE_ANN_NAME = createSimple( "org.jboss.logging.annotations.ValidIdRange" );
	public static final DotName MSG_ANN_NAME = createSimple( "org.jboss.logging.annotations.Message" );

	private final Property<RegularFile> reportFile;

	@Inject
	public LoggingReportTask(ProjectLayout layout, ObjectFactory objects) {
		super( objects );
		setDescription( "Generates a report of \"system\" logging" );
		reportFile = objects.fileProperty();
		reportFile.convention( layout.getBuildDirectory().file( "orm/generated/logging/index.adoc" ) );
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return reportFile;
	}

	@TaskAction
	public void generateLoggingReport() {
		final TreeMap<String, SubSystem> subSystemByName = new TreeMap<>();
		final TreeSet<IdRange> idRanges = new TreeSet<>( Comparator.comparing( IdRange::getMinValue ) );

		final Index index = getIndexManager().get().getIndex();
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
			final AnnotationInstance subSystemAnnUsage = loggerClassInfo.declaredAnnotation( SUB_SYS_ANN_NAME );

			final SubSystem subSystem;
			if ( subSystemAnnUsage != null ) {
				subSystem = subSystemByName.get( subSystemAnnUsage.value( "name" ).asString() );
			}
			else {
				subSystem = null;
			}

			final IdRange idRange;
			final AnnotationInstance idRangeAnnUsage = loggerClassInfo.declaredAnnotation( ID_RANGE_ANN_NAME );
			if ( idRangeAnnUsage == null ) {
				idRange = calculateIdRange( msgLoggerAnnUsage, subSystem );
			}
			else {
				idRange = new IdRange(
						asIntOrDefault( idRangeAnnUsage, "min" , 1 ),
						asIntOrDefault( idRangeAnnUsage, "max" , 999999 ),
						true,
						loggerClassInfo.simpleName(),
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
		getLogger().lifecycle( "MessageLogger (`{}`) missing id-range", loggerClassInfo.simpleName() );

		final List<AnnotationInstance> messageAnnUsages = loggerClassInfo.annotations( MSG_ANN_NAME );
		if ( messageAnnUsages.isEmpty() ) {
			return null;
		}

		int minId = Integer.MAX_VALUE;
		int maxId = Integer.MIN_VALUE;

		for ( int i = 0; i < messageAnnUsages.size(); i++ ) {
			final AnnotationInstance msgAnnUsage = messageAnnUsages.get( i );
			final int msgId = asIntOrDefault( msgAnnUsage, "id", -1 );

			if ( msgId < minId ) {
				minId = msgId;
			}
			else if ( msgId > maxId ) {
				maxId = msgId;
			}
		}

		return new IdRange( minId, maxId, false, loggerClassInfo.simpleName(), subSystem );
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
		try {
			fileWriter.write( "= Hibernate logging\n\n" );

			fileWriter.write( "[[subsystems]]\n" );
			fileWriter.write( "== Sub-system logging\n\n" );
			subSystemByName.forEach( (name, subSystem) -> {
				try {
					fileWriter.write( "[[" + subSystem.getAnchorName() + "]]\n" );
					fileWriter.write( "`" + subSystem.getName() + "`::\n" );
					fileWriter.write( "    * Logging class-name = `" + subSystem.getLoggingClassName() + "`\n" );
					fileWriter.write( "    * Description = " + subSystem.getDescription() + "\n" );
					if ( subSystem.getIdRange() != null ) {
						fileWriter.write( String.format(
								"    * ValidIdRange = <<%s,%s>>\n",
								subSystem.getIdRange().getAnchorName(),
								subSystem.getIdRange().getLabel()
						) );
					}
				}
				catch (IOException e) {
					throw new RuntimeException( "Error writing sub-system entry (" + subSystem.getAnchorName() + ") to report file", e );
				}
			} );

			fileWriter.write( "\n\n" );
			fileWriter.write( "[[id-ranges]]\n" );
			fileWriter.write( "== Message Id Ranges\n\n" );
			idRanges.forEach( (idRange) -> {
				try {
					fileWriter.write( "[[" + idRange.getAnchorName() + "]]\n" );
					fileWriter.write( "`" + idRange.getLabel() + "`::\n" );
					fileWriter.write( String.format(
							"    * ValidIdRange = %s - %s (%s)\n",
							idRange.minValueText,
							idRange.maxValueText,
							idRange.explicit ? "explicit" : "implicit"
					) );
					fileWriter.write( "    * MessageLogger = `" + idRange.getLoggerClassName() + "`\n" );
					final SubSystem subSystem = idRange.getSubSystem();
					if ( subSystem != null ) {
						fileWriter.write( String.format(
								"    * SubSystem = <<%s,%s>>\n",
								subSystem.getAnchorName(),
								subSystem.getName()
						) );
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
		private final boolean explicit;
		private final String loggerClassName;
		private final SubSystem subSystem;

		private final String minValueText;
		private final String maxValueText;

		public IdRange(
				int minValue,
				int maxValue,
				boolean explicit,
				String loggerClassName,
				SubSystem subSystem) {
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.explicit = explicit;
			this.loggerClassName = loggerClassName;
			this.subSystem = subSystem;

			this.minValueText = String.format( "HHH%06d", minValue );
			this.maxValueText = String.format( "HHH%06d", maxValue );
		}

		public int getMinValue() {
			return minValue;
		}

		public String getMinValueText() {
			return minValueText;
		}

		public int getMaxValue() {
			return maxValue;
		}

		public String getMaxValueText() {
			return maxValueText;
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
			return minValueText;
		}

		public String getLabel() {
			return minValueText + " - " + maxValueText;
		}
	}

	private int asIntOrDefault(AnnotationInstance instance, String param, int defaultValue) {
		final AnnotationValue value = instance.value( param );
		if ( value == null ) {
			getLogger().warn( "Explicit value for [{}] was not provided at {}! ", param, instance.target() );
			return defaultValue;
		}
		return value.asInt();
	}
}
