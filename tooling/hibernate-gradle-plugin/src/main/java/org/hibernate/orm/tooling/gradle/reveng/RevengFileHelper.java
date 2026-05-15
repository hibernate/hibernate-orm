/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

final class RevengFileHelper {

	private RevengFileHelper() {
	}

	static File findRequiredResourceFile(Project project, String filename) {
		final File result = findResourceFile( project, filename );
		if ( result == null ) {
			throw new BuildException( "File '" + filename + "' could not be found" );
		}
		return result;
	}

	static File findResourceFile(Project project, String filename) {
		final SourceSetContainer sourceSets = project.getExtensions().getByType( SourceSetContainer.class );
		final SourceSet sourceSet = sourceSets.getByName( SourceSet.MAIN_SOURCE_SET_NAME );
		final SourceDirectorySet resources = sourceSet.getResources();
		for ( File file : resources.getFiles() ) {
			if ( filename.equals( file.getName() ) ) {
				return file;
			}
		}
		return null;
	}

	static Properties loadPropertiesFile(Logger logger, File propertyFile) {
		logger.lifecycle( "Loading the properties file : " + propertyFile.getPath() );
		try ( FileInputStream inputStream = new FileInputStream( propertyFile ) ) {
			final Properties properties = new Properties();
			properties.load( inputStream );
			logger.lifecycle( "Properties file is loaded" );
			return properties;
		}
		catch (FileNotFoundException e) {
			throw new BuildException( propertyFile + " not found.", e );
		}
		catch (IOException e) {
			throw new BuildException( "Problem while loading " + propertyFile, e );
		}
	}
}
