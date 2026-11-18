package org.hibernate.build;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;
import java.io.File;
import java.util.Objects;

import static org.hibernate.build.HibernateVersion.fromVersionFile;

/**
 * @author Steve Ebersole
 */
public abstract class OrmBuildDetails {
	private final Provider<File> versionFileAccess;
	private final HibernateVersion hibernateVersion;

	private final JpaVersion jpaVersion;

	private final String databaseName;
	private final String databaseVersion;

	@Inject
	public OrmBuildDetails(Project project) {
		versionFileAccess = project.provider( () -> new File( project.getRootDir(), HibernateVersion.RELATIVE_FILE ) );

		hibernateVersion = fromVersionFile( versionFileAccess.get() );
		project.setVersion( hibernateVersion.getFullName() );

		jpaVersion = JpaVersion.from( project );

		databaseName = (String) project.property( "db" );
		databaseVersion = Objects.requireNonNullElse( (String) project.findProperty( "dbVersion" ), "latest" );
	}

	@InputFile
	public Provider<File> getVersionFileAccess() {
		return versionFileAccess;
	}

	@Internal
	public HibernateVersion getHibernateVersion() {
		return hibernateVersion;
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

	@Internal
	public JpaVersion getJpaVersion() {
		return jpaVersion;
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

	@Input
	public String getDatabaseVersion() {
		return databaseVersion;
	}

	@Input
	public String getDatabaseTag() {
		final String dbSuffix = databaseVersion == null ? "" : ("-" + databaseVersion);
		return getDatabaseName() + dbSuffix;
	}
}
