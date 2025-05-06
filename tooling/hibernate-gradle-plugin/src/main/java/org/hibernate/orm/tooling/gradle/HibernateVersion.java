/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;

import org.gradle.api.GradleException;

/**
 * @author Steve Ebersole
 */
public class HibernateVersion {
	public static volatile String version;

	static {
		version = determineHibernateVersion();
	}

	private static String determineHibernateVersion() {
		final URL versionFileUrl = findVersionFile();
		try ( final InputStream inputStream = versionFileUrl.openStream() ) {
			try ( final InputStreamReader inputStreamReader = new InputStreamReader( inputStream ) ) {
				return new LineNumberReader( inputStreamReader ).readLine();
			}
		}
		catch (IOException e) {
			throw new GradleException( "Unable to read `META-INF/hibernate-orm.version` resource" );
		}
	}

	private static URL findVersionFile() {
		final URL badGunsAndRoses = HibernateOrmSpec.class.getClassLoader().getResource( "META-INF/hibernate-orm.version" );
		if ( badGunsAndRoses != null ) {
			return badGunsAndRoses;
		}

		//noinspection UnnecessaryLocalVariable
		final URL goodGunsAndRoses = HibernateOrmSpec.class.getClassLoader().getResource( "/META-INF/hibernate-orm.version" );

		return goodGunsAndRoses;
	}
}
