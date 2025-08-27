/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Steve Ebersole
 */
public class Copier {
	public static void copyProject(String projectBuildFilePath, Path target) {
		final URL resource = Copier.class.getClassLoader().getResource( "projects/" + projectBuildFilePath );
		if ( resource == null ) {
			throw new RuntimeException( "Unable to locate projectBuildFilePath : " + projectBuildFilePath );
		}

		try {
			final Path sourceProjectDir = Paths.get( resource.toURI() );
			copyDirectory( sourceProjectDir.getParent(), target );
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( "Unable to create URI : " + resource, e );
		}
	}

	public static void copyDirectory(Path source, Path target) {
		try {
			Files.walk( source ).forEach( (sourceItem) -> {
				if ( sourceItem.equals( source ) ) {
					return;
				}

				final Path output = target.resolve( source.relativize( sourceItem ) );
				try {
					Files.copy( sourceItem, output );
				}
				catch (IOException ioe) {
					throw new RuntimeException( "Unable to copy : " + sourceItem.toAbsolutePath(), ioe );
				}
			} );
		}
		catch (IOException ioe) {
			throw new RuntimeException( "Unable to copy : " + source.toAbsolutePath() );
		}
	}
}
