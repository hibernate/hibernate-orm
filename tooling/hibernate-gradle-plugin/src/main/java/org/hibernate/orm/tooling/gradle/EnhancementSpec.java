/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for configuring bytecode enhancement - available as `project.hibernateOrm.enhancement`
 */
@SuppressWarnings( { "unused", "RedundantSuppression" } )
public class EnhancementSpec {
	public static final String ENHANCE = "enhance";
	public static final String ENHANCEMENT = "enhancement";

	public static final String DSL_NAME = ENHANCEMENT;

	private final Project project;

	private final DirectoryProperty outputDirectory;
	private final Property<Boolean> enableLazyInitialization;
	private final Property<Boolean> enableDirtyTracking;
	private final Property<Boolean> enableAssociationManagement;
	private final Property<Boolean> enableExtendedEnhancement;


	@Inject
	public EnhancementSpec(HibernateOrmSpec ormDsl, Project project) {
		this.project = project;

		outputDirectory = makeOutputDirectory( ormDsl, project );
		enableLazyInitialization = makeProperty( project );
		enableDirtyTracking = makeProperty( project );
		enableAssociationManagement = makeProperty( project );
		enableExtendedEnhancement = makeProperty( project );
	}

	public boolean hasAnythingToDo() {
		return enableLazyInitialization.get()
				|| enableDirtyTracking.get()
				|| enableAssociationManagement.get()
				|| enableExtendedEnhancement.get();
	}

	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(Directory directory) {
		outputDirectory.set( directory );
	}

	public void outputDirectory(Directory directory) {
		setOutputDirectory( directory );
	}

	public void setOutputDirectory(String path) {
		outputDirectory.set( project.getLayout().getBuildDirectory().dir( path ) );
	}

	public void outputDirectory(String path) {
		setOutputDirectory( path );
	}

	public Property<Boolean> getEnableLazyInitialization() {
		return enableLazyInitialization;
	}

	public void setEnableLazyInitialization(boolean enable) {
		enableLazyInitialization.set( enable );
	}

	public void enableLazyInitialization(boolean enable) {
		setEnableLazyInitialization( enable );
	}

	public void lazyInitialization(boolean enable) {
		setEnableLazyInitialization( enable );
	}

	public void setLazyInitialization(boolean enable) {
		setEnableLazyInitialization( enable );
	}


	public Property<Boolean> getEnableDirtyTracking() {
		return enableDirtyTracking;
	}

	public void setEnableDirtyTracking(boolean enable) {
		enableDirtyTracking.set( enable );
	}

	public void enableDirtyTracking(boolean enable) {
		setEnableDirtyTracking( enable );
	}

	public void dirtyTracking(boolean enable) {
		setEnableDirtyTracking( enable );
	}

	public void setDirtyTracking(boolean enable) {
		setEnableDirtyTracking( enable );
	}


	public Property<Boolean> getEnableAssociationManagement() {
		return enableAssociationManagement;
	}

	public void setEnableAssociationManagement(boolean enable) {
		enableAssociationManagement.set( enable );
	}

	public void enableAssociationManagement(boolean enable) {
		setEnableAssociationManagement( enable );
	}

	public void associationManagement(boolean enable) {
		setEnableAssociationManagement( enable );
	}


	public Property<Boolean> getEnableExtendedEnhancement() {
		return enableExtendedEnhancement;
	}

	public void setEnableExtendedEnhancement(boolean enable) {
		enableExtendedEnhancement.set( enable );
	}

	public void enableExtendedEnhancement(boolean enable) {
		setEnableExtendedEnhancement( enable );
	}

	public void extendedEnhancement(boolean enable) {
		setEnableExtendedEnhancement( enable );
	}

	@SuppressWarnings( "UnstableApiUsage" )
	public static Property<Boolean> makeProperty(Project project) {
		final Property<Boolean> createdProperty = project.getObjects().property( Boolean.class );
		// default to false
		createdProperty.convention( false );
		return createdProperty;
	}

	@SuppressWarnings( "UnstableApiUsage" )
	public static DirectoryProperty makeOutputDirectory(HibernateOrmSpec ormDsl, Project project) {
		final DirectoryProperty outputDirectory = project.getObjects().directoryProperty();
		// default `{buildDir}/hibernate/orm/enhance
		outputDirectory.convention( ormDsl.getOutputDirectory().dir( "enhance" ) );

		return outputDirectory;
	}
}
