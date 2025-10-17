/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;

import javax.inject.Inject;
import java.io.File;

import static org.hibernate.build.HibernateVersion.fromVersionFile;

/**
 * @author Steve Ebersole
 */
public abstract class OrmBuildDetails {
	private final ReleaseDetails releaseDetails;
	private final Provider<File> versionFileAccess;
	private final HibernateVersion hibernateVersion;

	private final JpaVersion jpaVersion;

	private final String databaseName;

	@Inject
	public OrmBuildDetails(Project project) {
		releaseDetails = new ReleaseDetails( project );
		versionFileAccess = project.provider( () -> new File( project.getRootDir(), HibernateVersion.RELATIVE_FILE ) );

		hibernateVersion = releaseDetails.getReleaseVersion() != null
				? releaseDetails.getReleaseVersion()
				: fromVersionFile( versionFileAccess.get() );
		project.setVersion( hibernateVersion.getFullName() );

		jpaVersion = JpaVersion.from( project );

		databaseName = (String) project.property( "db" );
	}

	@InputFile
	public Provider<File> getVersionFileAccess() {
		return versionFileAccess;
	}

	@Nested
	public ReleaseDetails getReleaseDetails() {
		return releaseDetails;
	}

	@Input
	public String getHibernateVersionName() {
		return getHibernateVersion().getFullName();
	}

	@Input
	public String getHibernateVersionFamily() {
		return getHibernateVersion().getFamily();
	}

	@Input
	public String getHibernateVersionNameOsgi() {
		return getHibernateVersion().getOsgiVersion();
	}

	@Input
	public String getJpaVersionName() {
		return getJpaVersion().getName();
	}

	@Input
	public String getJpaVersionNameOsgi() {
		return getJpaVersion().getOsgiName();
	}

	@Input
	public String getDatabaseName() {
		return databaseName;
	}

	@Internal
	public HibernateVersion getHibernateVersion() {
		return hibernateVersion;
	}

	@Internal
	public JpaVersion getJpaVersion() {
		return jpaVersion;
	}
}
