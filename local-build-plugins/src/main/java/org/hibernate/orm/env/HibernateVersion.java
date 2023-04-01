/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.function.Consumer;

import org.gradle.api.Project;

/**
 * @author Steve Ebersole
 */
public class HibernateVersion implements Serializable {
	public static final String EXT_KEY = "ormVersion";
	public static final String VERSION_KEY = "releaseVersion";
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

	public static HibernateVersion from(Project project, File versionFile) {
		if ( project.hasProperty( VERSION_KEY ) ) {
			final Object version = project.property( VERSION_KEY );
			if ( version != null ) {
				return new HibernateVersion( (String) version );
			}
		}

		final String fullName = readVersionProperties( versionFile );
		return new HibernateVersion( fullName );
	}

	private static String readVersionProperties(File file) {
		if ( !file.exists() ) {
			throw new RuntimeException( "Version file $file.canonicalPath does not exists" );
		}

		final Properties versionProperties = new Properties();
		withInputStream( file, (stream) -> {
			try {
				versionProperties.load( stream );
			}
			catch (IOException e) {
				throw new RuntimeException( "Unable to load properties from file - " + file.getAbsolutePath(), e );
			}
		} );

		return versionProperties.getProperty( "hibernateVersion" );
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
