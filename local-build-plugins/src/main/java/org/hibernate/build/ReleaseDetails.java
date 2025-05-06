/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

/**
 * @author Steve Ebersole
 */
public class ReleaseDetails {
	private final HibernateVersion releaseVersion;
	private final HibernateVersion developmentVersion;
	private final Provider<Boolean> createTagAccess;
	private final Provider<String> tagNameAccess;

	public ReleaseDetails(Project project) {
		final Object releaseVersionSetting = project.findProperty( "releaseVersion" );
		final Object developmentVersionSetting = project.findProperty( "developmentVersion" );
		if ( releaseVersionSetting != null ) {
			if ( developmentVersionSetting == null ) {
				throw new IllegalStateException( "`releaseVersion` with no `developmentVersion`" );
			}
			releaseVersion = new HibernateVersion( releaseVersionSetting.toString() );
			developmentVersion = new HibernateVersion( developmentVersionSetting.toString() );
			tagNameAccess = project.provider( () -> determineReleaseTag( releaseVersion.getFullName() ) );
		}
		else {
			releaseVersion = null;
			developmentVersion = null;
			tagNameAccess = project.provider( () -> null );
		}

		createTagAccess = project.provider( () -> !project.hasProperty( "noTag" ) );
	}

	public HibernateVersion getReleaseVersion() {
		return releaseVersion;
	}

	public HibernateVersion getDevelopmentVersion() {
		return developmentVersion;
	}

	public Provider<Boolean> getCreateTagAccess() {
		return createTagAccess;
	}

	public boolean shouldCreateTag() {
		return getCreateTagAccess().get();
	}

	public Provider<String> getTagNameAccess() {
		return tagNameAccess;
	}

	public String getTagNameToUse() {
		return getTagNameAccess().get();
	}

	private static String determineReleaseTag(String releaseVersion) {
		return releaseVersion.endsWith( ".Final" )
				? releaseVersion.replace( ".Final", "" )
				: releaseVersion;
	}
}
