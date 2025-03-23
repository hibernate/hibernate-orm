/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.aspects;

import org.gradle.api.Project;
import org.hibernate.build.OrmBuildDetails;

import java.util.Map;

/**
 * Any of the projects
 *
 * @author Steve Ebersole
 */
public class ModuleAspect implements Aspect {
	@Override
	public void apply(Project target) {
		target.getPluginManager().apply( "base" );

		// read-only details about the build
		final OrmBuildDetails ormBuildDetails = target.getExtensions().create("ormBuildDetails",
				OrmBuildDetails.class,
				target
		);

		target.setGroup( "org.hibernate.orm" );
		target.setVersion( ormBuildDetails.getHibernateVersionName() );

		target.getLayout().getBuildDirectory().set( target.getLayout().getProjectDirectory().dir( "target" ) );

		target.getExtensions().add( "jakartaJpaVersion", ormBuildDetails.getJpaVersionNameOsgi() );
		target.getExtensions().add( "db", ormBuildDetails.getDatabaseName() );

		target.getConfigurations().configureEach( (files) -> {
			files.exclude( Map.of( "group", "xml-apis", "module", "xml-apis" ) );
		} );
	}
}
