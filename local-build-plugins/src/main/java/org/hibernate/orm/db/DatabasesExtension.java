/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.db;

import javax.inject.Inject;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;

/**
 * @author Steve Ebersole
 */
public abstract class DatabasesExtension {
	public static final String DSL_NAME = "databases";

	private final Project project;

	private final Property<DatabaseConfig> testingDatabase;
	private final NamedDomainObjectContainer<DatabaseConfig> availableDatabases;

	@Inject
	public DatabasesExtension(Project project) {
		this.project = project;

		this.availableDatabases = project.getObjects().domainObjectContainer( DatabaseConfig.class, new DatabaseConfigFactory( project ) );

		this.testingDatabase = project.getObjects().property( DatabaseConfig.class );
		this.testingDatabase.convention( project.provider( () -> availableDatabases.getByName(  "h2" ) ) );
	}

	@Input
	@Nested
	public NamedDomainObjectContainer<DatabaseConfig> getAvailableDatabases() {
		return availableDatabases;
	}

//	@Input
//	public Property<String> getTestingDatabase() {
//		return testingDatabase;
//	}
//
//	public void setTestingDatabase(String configName) {
//		testingDatabase.set( configName );
//	}
//
//	public void setTestingDatabase(Provider<String> configName) {
//		testingDatabase.set( configName );
//	}
//
//	@Internal
//	public DatabaseConfig getEffectiveDatabase() {
//		return getAvailableDatabases().getByName( testingDatabase.get() );
//	}


	public Property<DatabaseConfig> getTestingDatabase() {
		return testingDatabase;
	}

	public void setTestingDatabase(String databaseName) {
		testingDatabase.convention( availableDatabases.named( databaseName ) );
//		effectiveDatabase.convention( project.provider( () -> bundles.getByName( databaseName ) ) );
	}

	public void setTestingDatabase(Provider<String> databaseName) {
		testingDatabase.set( databaseName.flatMap( availableDatabases::named ) );
	}

}
