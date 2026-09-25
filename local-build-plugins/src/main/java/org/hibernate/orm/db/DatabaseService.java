package org.hibernate.orm.db;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

public abstract class DatabaseService implements BuildService<BuildServiceParameters.None> {
	public static final String REGISTRATION_NAME = "databaseService";
}
