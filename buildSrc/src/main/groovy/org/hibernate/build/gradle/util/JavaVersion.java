/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.build.gradle.util;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class JavaVersion {
	public static enum Family {
        JAVA7( 7 ),
		JAVA6( 6 ),
		JAVA5( 5 );

		private final int code;

		private Family(int code) {
			this.code = code;
		}
	}

	private final String fullVersionString;
	private final Family family;

	public JavaVersion(String javaVersionString) {
		this.fullVersionString = javaVersionString;
		family = fullVersionString.startsWith( "1.6" )
				? Family.JAVA6
				: ( fullVersionString.startsWith( "1.7" ) ? Family.JAVA7 : Family.JAVA5);
	}

	public String getFullVersionString() {
		return fullVersionString;
	}

	public Family getFamily() {
		return family;
	}

	public boolean isAtLeast(Family family) {
		return getFamily().code >= family.code;
	}
}
