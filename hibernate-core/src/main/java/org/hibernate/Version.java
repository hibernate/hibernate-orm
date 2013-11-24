/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate;

import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Information about the Hibernate version.
 *
 * @author Steve Ebersole
 */
public class Version {
	private Version() {
	}

	/**
	 * Access to the Hibernate version.
	 *
	 * IMPL NOTE : Real value is injected by the build.
	 *
	 * @return The Hibernate version
	 */
	public static String getVersionString() {
		return "[WORKING]";
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
	public static void main(String[] args) {
		System.out.println( "Hibernate Core {" + getVersionString() + "}" );
	}
}
