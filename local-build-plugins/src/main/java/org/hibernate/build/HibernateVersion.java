/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public class HibernateVersion implements Serializable {
	public static final String RELATIVE_FILE = "gradle/version.properties";

	private final String fullName;
	private final String majorVersion;
	private final String family;

	private final String osgiVersion;

	private final boolean isSnapshot;

	HibernateVersion(String fullName) {
		this.fullName = fullName;

		final String[] hibernateVersionComponents = fullName.split( "\\." );
		this.majorVersion = hibernateVersionComponents[0];
		this.family = hibernateVersionComponents[0] + '.' + hibernateVersionComponents[1];

		this.isSnapshot = fullName.endsWith( "-SNAPSHOT" );

		this.osgiVersion = isSnapshot ? family + '.' + hibernateVersionComponents[2] + ".SNAPSHOT" : fullName;
	}

	public String getFullName() {
		return fullName;
	}

	public String getMajorVersion() {
		return majorVersion;
	}

	public String getFamily() {
		return family;
	}

	public String getOsgiVersion() {
		return osgiVersion;
	}

	public boolean isSnapshot() {
		return isSnapshot;
	}

	public static HibernateVersion fromVersionFile(File versionFile) {
		if ( !versionFile.exists() ) {
			throw new RuntimeException( "Version file $file.canonicalPath does not exists" );
		}

		final Properties versionProperties = new Properties();
		withInputStream( versionFile, (stream) -> {
			try {
				versionProperties.load( stream );
			}
			catch (IOException e) {
				throw new RuntimeException( "Unable to load properties from file - " + versionFile.getAbsolutePath(), e );
			}
		} );

		return new HibernateVersion( versionProperties.getProperty( "hibernateVersion" ) );
	}

	private static void withInputStream(File file, Consumer<InputStream> action) {
		try ( final FileInputStream stream = new FileInputStream( file ) ) {
			action.accept( stream );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error reading file stream = " + file.getAbsolutePath(), e );
		}
	}

	@Override
	public String toString() {
		return fullName;
	}
}
