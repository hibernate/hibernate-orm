/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import java.io.File;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.orm.ReleaseFamilyIdentifier;

/**
 * @author Steve Ebersole
 */
public abstract class GenerateDescriptorTask extends DefaultTask {
	public static final String GEN_DESC_TASK_NAME = "generateDocumentationDescriptor";
	private final RegularFileProperty jsonFile;
	private final Property<ReleaseFamilyIdentifier> currentlyBuildingFamily;

	public GenerateDescriptorTask() {
		setGroup( "documentation" );
		setDescription( "Generates the documentation publication descriptor (JSON)" );

		jsonFile = getProject().getObjects().fileProperty();
		currentlyBuildingFamily = getProject().getObjects().property( ReleaseFamilyIdentifier.class );
	}

	@Input
	public Property<ReleaseFamilyIdentifier> getCurrentlyBuildingFamily() {
		return currentlyBuildingFamily;
	}

	@OutputFile
	public RegularFileProperty getJsonFile() {
		return jsonFile;
	}

	@TaskAction
	public void generateDescriptor() {
		final ProjectDocumentationDescriptor descriptor = DescriptorAccess.loadProject();

		ReleaseFamilyIdentifier newest = null;
		boolean foundCurrentRelease = false;

		for ( ReleaseFamilyDocumentation releaseFamily : descriptor.getReleaseFamilies() ) {
			if ( newest == null
					|| releaseFamily.getName().newerThan( newest ) ) {
				newest = releaseFamily.getName();
			}

			if ( releaseFamily.getName().equals( currentlyBuildingFamily.get() ) ) {
				foundCurrentRelease = true;
			}
		}

		if ( ! foundCurrentRelease ) {
			final ReleaseFamilyDocumentation newEntry = new ReleaseFamilyDocumentation();
			newEntry.setName( currentlyBuildingFamily.get() );
			descriptor.addReleaseFamily( newEntry );
			setDidWork( true );
		}

		// we only want to update "stable" to `currentlyBuildingFamily` when-
		// 		1. we are currently building a Final
		//		2. currentlyBuildingFamily is the newest

		if ( currentlyBuildingFamily.get().newerThan( newest ) ) {
			descriptor.setStableFamily( currentlyBuildingFamily.get() );
			setDidWork( true );
		}

		DescriptorAccess.storeProject( descriptor, jsonFile.get().getAsFile() );
	}

	public static void main(String... args) {
		final File jsonFile = new File( "/home/sebersole/projects/hibernate-orm/6.0/hibernate-orm-build/target/doc-pub/orm.json" );
		final ProjectDocumentationDescriptor projectDoc = DescriptorAccess.loadProject();
		DescriptorAccess.storeProject( projectDoc, jsonFile );
	}
}
