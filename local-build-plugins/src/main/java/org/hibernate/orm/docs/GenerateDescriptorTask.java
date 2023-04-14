/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import java.io.File;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.orm.ReleaseFamilyIdentifier;

/**
 * @author Steve Ebersole
 */
public abstract class GenerateDescriptorTask extends DefaultTask {
	private final Provider<RegularFile> jsonFile;
	private final ReleaseFamilyIdentifier currentlyBuildingFamily;

	@Inject
	public GenerateDescriptorTask(DocumentationPublishing config) {
		setGroup( "Release" );
		setDescription( "Generates the documentation publication descriptor (JSON)" );

		jsonFile = config.getUpdatedJsonFile();
		currentlyBuildingFamily = config.getReleaseFamilyIdentifier();

		getInputs().property( "hibernate-version", currentlyBuildingFamily );
	}

	@OutputFile
	public Provider<RegularFile> getJsonFile() {
		return jsonFile;
	}

	@TaskAction
	public void generateDescriptor() {
		final ProjectDocumentation projectDoc = DescriptorAccess.loadProject();

		ReleaseFamilyIdentifier newest = null;
		boolean foundCurrentRelease = false;

		for ( ReleaseFamilyDocumentation releaseFamily : projectDoc.getReleaseFamilies() ) {
			if ( newest == null
					|| releaseFamily.getName().newerThan( newest ) ) {
				newest = releaseFamily.getName();
			}

			if ( releaseFamily.getName().equals( currentlyBuildingFamily ) ) {
				foundCurrentRelease = true;
			}
		}

		if ( ! foundCurrentRelease ) {
			final ReleaseFamilyDocumentation newEntry = new ReleaseFamilyDocumentation();
			newEntry.setName( currentlyBuildingFamily );
			setDidWork( true );
		}

		// we only want to update "stable" to `currentlyBuildingFamily` when-
		// 		1. we are currently building a Final
		//		2. currentlyBuildingFamily is the newest

		if ( currentlyBuildingFamily.newerThan( newest ) ) {
			projectDoc.setStableFamily( currentlyBuildingFamily );
			setDidWork( true );
		}

		DescriptorAccess.storeProject( projectDoc, jsonFile.get().getAsFile() );
	}

	public static void main(String... args) {
		final File jsonFile = new File( "/home/sebersole/projects/hibernate-orm/6.0/hibernate-orm-build/target/doc-pub/orm.json" );
		final ProjectDocumentation projectDoc = DescriptorAccess.loadProject();
		DescriptorAccess.storeProject( projectDoc, jsonFile );
	}
}
