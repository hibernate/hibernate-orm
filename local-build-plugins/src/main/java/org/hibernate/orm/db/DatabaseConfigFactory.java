/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.db;

import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;

/**
 * Factory for {@link DatabaseConfig} instances as part of {@link DatabasesExtension#getAvailableDatabases()}
 *
 * @author Steve Ebersole
 */
public class DatabaseConfigFactory implements NamedDomainObjectFactory<DatabaseConfig> {
	private final Project project;

	public DatabaseConfigFactory(Project project) {
		this.project = project;
	}

	@Override
	public DatabaseConfig create(String name) {
		return new DatabaseConfig( name, project );
	}
}
