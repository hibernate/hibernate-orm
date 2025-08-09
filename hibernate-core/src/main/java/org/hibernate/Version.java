/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.build.AllowSysOut;

import java.lang.invoke.MethodHandles;
import java.lang.module.ModuleDescriptor;

import static org.jboss.logging.Logger.getMessageLogger;

/**
 * Information about the version of Hibernate.
 *
 * @author Steve Ebersole
 */
public final class Version {

	private static final String VERSION = initVersion();

	private static String initVersion() {
		ModuleDescriptor moduleDescriptor = Version.class.getModule().getDescriptor() ;
		return moduleDescriptor != null ? (moduleDescriptor.version().isPresent() ? moduleDescriptor.version().toString() : "[WORKING]") : "[WORKING]";
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
		getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, Version.class.getName() )
				.version( getVersionString() );
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
