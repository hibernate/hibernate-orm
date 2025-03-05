/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.docs;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.orm.ReleaseFamilyIdentifier;

/**
 * Task for creating the JSON "documentation descriptor" for ORM
 *
 * @author Steve Ebersole
 */
public abstract class GenerateDescriptorTask extends DefaultTask {
	public static final String GEN_DESC_TASK_NAME = "generateDocumentationDescriptor";

	private final Property<ReleaseFamilyIdentifier> currentlyBuildingFamily;
	private final RegularFileProperty jsonFile;

	private boolean needsUpload;
	private boolean needsSymLinkUpdate;

	public GenerateDescriptorTask() {
		setGroup( "documentation" );
		setDescription( "Generates the documentation publication descriptor (JSON)" );

		currentlyBuildingFamily = getProject().getObjects().property( ReleaseFamilyIdentifier.class );
		jsonFile = getProject().getObjects().fileProperty();
	}

	@Input
	public Property<ReleaseFamilyIdentifier> getCurrentlyBuildingFamily() {
		return currentlyBuildingFamily;
	}

	@OutputFile
	public RegularFileProperty getJsonFile() {
		return jsonFile;
	}

	/**
	 * Whether we determined, during {@linkplain #generateDescriptor}, that uploading the
	 * doc descriptor was needed.
	 *
	 * @see PublishDescriptorTask
	 */
	public boolean needsUpload() {
		return getDidWork() && needsUpload;
	}

	/**
	 * Whether we determined, during {@linkplain #generateDescriptor}, that updating the
	 * doc server symlinks was needed.
	 *
	 * @see UpdateSymLinksTask
	 */
	public boolean needsSymLinkUpdate() {
		return getDidWork() && needsSymLinkUpdate;
	}

	@TaskAction
	public void generateDescriptor() {
		final ProjectDocumentationDescriptor descriptor = DescriptorAccess.loadProject();
		final boolean isFinal = getProject().getVersion().toString().endsWith( ".Final" );

		final Set<ReleaseFamilyIdentifier> processedReleases = new HashSet<>();
		ReleaseFamilyIdentifier newest = null;
		boolean foundCurrentRelease = false;

		final Iterator<ReleaseFamilyDocumentation> itr = descriptor.getReleaseFamilies().iterator();
		while ( itr.hasNext() ) {
			final ReleaseFamilyDocumentation releaseFamily = itr.next();

			// NOTE: sometimes releases get duplicated in the descriptor...
			// let's clean those up if we run across them
			if ( !processedReleases.add( releaseFamily.getName() ) ) {
				itr.remove();
				needsUpload = true;
				continue;
			}

			if ( newest == null
					|| releaseFamily.getName().newerThan( newest ) ) {
				newest = releaseFamily.getName();
			}

			if ( releaseFamily.getName().equals( currentlyBuildingFamily.get() ) ) {
				foundCurrentRelease = true;
			}
		}

		if ( isFinal ) {
			// we are releasing a Final - possibly do some other things

			if ( !foundCurrentRelease ) {
				// this release is not yet tracked in the descriptor - add it
				final ReleaseFamilyDocumentation newEntry = new ReleaseFamilyDocumentation();
				newEntry.setName( currentlyBuildingFamily.get() );
				descriptor.addReleaseFamily( newEntry );
				setDidWork( true );
				needsUpload = true;
			}

			if ( currentlyBuildingFamily.get().newerThan( newest ) ) {
				// this release is newer than any currently tracked in the descriptor
				descriptor.setStableFamily( currentlyBuildingFamily.get() );
				setDidWork( true );
				needsSymLinkUpdate = true;
			}
		}

		DescriptorAccess.storeProject( descriptor, jsonFile.get().getAsFile() );
	}

	public static void main(String... args) {
		final File jsonFile = new File( "/tmp/hibernate-orm-build/doc-pub/orm.json" );
		final ProjectDocumentationDescriptor projectDoc = DescriptorAccess.loadProject();
		DescriptorAccess.storeProject( projectDoc, jsonFile );
	}
}
