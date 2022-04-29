package org.hibernate.orm.env;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author Steve Ebersole
 */
public class EnvironmentProjectPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		final File versionFile = project.getRootProject().file( HibernateVersion.RELATIVE_FILE );
		project.getExtensions().add( "ormVersionFile", versionFile );
		project.getExtensions().add( HibernateVersion.EXT_KEY, HibernateVersion.from( project, versionFile ) );
		project.getExtensions().add( JpaVersion.EXT_KEY, JpaVersion.from( project ) );
		project.getExtensions().add( "jpaVersion", new JpaVersion( "2.2" ) );
	}
}
