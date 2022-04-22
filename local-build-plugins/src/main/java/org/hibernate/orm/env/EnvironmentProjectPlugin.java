package org.hibernate.orm.env;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author Steve Ebersole
 */
public class EnvironmentProjectPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getExtensions().add( HibernateVersion.EXT_KEY, HibernateVersion.from( project ) );
		project.getExtensions().add( JpaVersion.EXT_KEY, JpaVersion.from( project ) );

		project.getExtensions().add( "jpaVersion", new JpaVersion( "2.2" ) );
	}
}
