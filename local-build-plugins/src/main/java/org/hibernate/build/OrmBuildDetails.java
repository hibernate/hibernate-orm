/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.io.File;

import static org.hibernate.build.HibernateVersion.fromVersionFile;

/**
 * @author Steve Ebersole
 */
public abstract class OrmBuildDetails {
	private final Provider<File> versionFileAccess;
	private final HibernateVersion hibernateVersion;

	private final JpaVersion jpaVersion;

	private final String databaseName;

	@Inject
	public OrmBuildDetails(Project project) {
		versionFileAccess = project.provider( () -> new File( project.getRootDir(), HibernateVersion.RELATIVE_FILE ) );

		hibernateVersion = fromVersionFile( versionFileAccess.get() );
		project.setVersion( hibernateVersion.getFullName() );

		jpaVersion = JpaVersion.from( project );

		databaseName = (String) project.property( "db" );
	}

	public Provider<File> getVersionFileAccess() {
		return versionFileAccess;
	}

	public HibernateVersion getHibernateVersion() {
		return hibernateVersion;
	}

	public String getHibernateVersionName() {
		return getHibernateVersion().getFullName();
	}

	public String getHibernateVersionFamily() {
		return getHibernateVersion().getFamily();
	}

	public String getHibernateVersionNameOsgi() {
		return getHibernateVersion().getOsgiVersion();
	}

	public JpaVersion getJpaVersion() {
		return jpaVersion;
	}

	public String getJpaVersionName() {
		return getJpaVersion().getName();
	}

	public String getJpaVersionNameOsgi() {
		return getJpaVersion().getOsgiName();
	}

	public String getDatabaseName() {
		return databaseName;
	}
}
