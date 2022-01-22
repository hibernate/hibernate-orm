/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import java.util.List;
import java.util.TreeSet;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;

/**
 * @author Steve Ebersole
 */
public abstract class IncubatingCollectorTask extends DefaultTask {
	public static final String INCUBATING_ANN_NAME = "org.hibernate.Incubating";

	private final IndexManager indexManager;
	private final Provider<RegularFile> reportFileReferenceAccess;

	@Inject
	public IncubatingCollectorTask(IndexManager indexManager, Project project) {
		this.indexManager = indexManager;
		this.reportFileReferenceAccess = project.getLayout().getBuildDirectory().file( "post/" + project.getName() + "-incubating.txt" );
	}

	@InputFile
	public Provider<RegularFile> getIndexFileReference() {
		return indexManager.getIndexFileReferenceAccess();
	}

	@OutputFile
	public Provider<RegularFile> getReportFileReferenceAccess() {
		return reportFileReferenceAccess;
	}

	@TaskAction
	public void collectIncubationDetails() {
		final Index index = indexManager.getIndex();

		final List<AnnotationInstance> usages = index.getAnnotations( DotName.createSimple( INCUBATING_ANN_NAME ) );
		final TreeSet<String> usagePathSet = new TreeSet<>();
		usages.forEach( (usage) -> {
			final AnnotationTarget usageLocation = usage.target();
			final String locationPath = determinePath( usageLocation );
			if ( locationPath != null ) {
				usagePathSet.add( locationPath );
			}
		} );

		// at this point, `usagePathSet` contains a set of incubating names ordered alphabetically..
		String previousPath = null;
		for ( String usagePath : usagePathSet ) {
			if ( previousPath != null && usagePath.startsWith( previousPath ) ) {
				continue;
			}

			// `usagePath` is a path we want to document
			collectIncubation( usagePath );

			previousPath = usagePath;
		}
	}

	private void collectIncubation(String usagePath) {
		// todo - what to do with this?
	}

	private String determinePath(AnnotationTarget usageLocation) {
		switch ( usageLocation.kind() ) {
			case CLASS: {
				final DotName name = usageLocation.asClass().name();
				if ( name.local().equals( "package-info.class" ) ) {
					return name.packagePrefix();
				}
				return name.toString();
			}
			case FIELD: {
				final FieldInfo fieldInfo = usageLocation.asField();
				return fieldInfo.declaringClass().name().toString()
						+ "#"
						+ fieldInfo.name();
			}
			case METHOD: {
				final MethodInfo methodInfo = usageLocation.asMethod();
				return methodInfo.declaringClass().name().toString()
						+ "#"
						+ methodInfo.name();
			}
			default: {
				return null;
			}
		}
	}
}
