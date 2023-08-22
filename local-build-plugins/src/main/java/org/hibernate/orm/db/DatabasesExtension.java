/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.db;

import java.util.Map;
import javax.inject.Inject;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;

/**
 * @author Steve Ebersole
 */
public abstract class DatabasesExtension {
	public static final String DSL_NAME = "testingDatabases";

	private final Project project;

	private final Property<DatabaseConfig> effective;
	private final NamedDomainObjectContainer<DatabaseConfig> available;

	@Inject
	public DatabasesExtension(Project project) {
		this.project = project;

		this.available = project.getObjects().domainObjectContainer( DatabaseConfig.class, new DatabaseConfigFactory( project ) );

		this.effective = project.getObjects().property( DatabaseConfig.class );
		this.effective.convention( project.provider( () -> available.getByName( "h2" ) ) );
	}

	@Input
	public NamedDomainObjectContainer<DatabaseConfig> getAvailable() {
		return available;
	}

	@Input
	@Nested
	public Property<DatabaseConfig> getEffective() {
		return effective;
	}

	public void setEffective(String databaseName) {
		effective.convention( project.provider( () -> available.getByName( databaseName ) ) );
	}

	public void setTestingDatabase(Provider<String> databaseName) {
		effective.set( databaseName.flatMap( available::named ) );
	}

	@Internal
	public String getEffectiveDatabaseName() {
		return effective.get().getName();
	}

	@Internal
	public Map<String, String> getEffectiveDatabaseSettings() {
		return effective.get().getSettings();
	}
}
