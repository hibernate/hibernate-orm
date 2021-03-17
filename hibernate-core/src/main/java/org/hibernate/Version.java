/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.build.AllowSysOut;

import org.jboss.logging.Logger;

/**
 * Information about the Hibernate version.
 *
 * @author Steve Ebersole
 */
public final class Version {

	private static final String VERSION = initVersion();

	private static String initVersion() {
		final String version = Version.class.getPackage().getImplementationVersion();
		return version != null ? version : "[WORKING]";
	}

	private Version() {
	}

	/**
	 * Access to the Hibernate ORM version.
	 *
	 * @return The Hibernate version
	 */
	public static String getVersionString() {
		return VERSION;
	}

	/**
	 * Logs the Hibernate version (using {@link #getVersionString()}) to the logging system.
	 */
	public static void logVersion() {
		Logger.getMessageLogger( CoreMessageLogger.class, Version.class.getName() ).version( getVersionString() );
	}

	/**
	 * Prints the Hibernate version (using {@link #getVersionString()}) to SYSOUT.  Defined as the main-class in
	 * the hibernate-core jar
	 *
	 * @param args n/a
	 */
	@AllowSysOut
	public static void main(String[] args) {
		System.out.println( "Hibernate ORM core version " + getVersionString() );
	}
}
