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
package org.hibernate.metamodel.source.hbm.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.MappingException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;

/**
 * Helper class.
 *
 * @author Gail Badner
 */
public class MappingHelper {
	private MappingHelper() {
	}

	public static String getStringValue(String value, String defaultValue) {
		return value == null ? defaultValue : value;
	}

	public static int getIntValue(String value, int defaultValue) {
		return value == null ? defaultValue : Integer.parseInt( value );
	}

	public static boolean getBooleanValue(String value, boolean defaultValue) {
		return value == null ? defaultValue : Boolean.valueOf( value );
	}

	public static boolean getBooleanValue(Boolean value, boolean defaultValue) {
		return value == null ? defaultValue : value;
	}

	public static Set<String> getStringValueTokens(String str, String delimiters) {
		if ( str == null ) {
			return Collections.emptySet();
		}
		else {
			StringTokenizer tokenizer = new StringTokenizer( str, delimiters );
			Set<String> tokens = new HashSet<String>();
			while ( tokenizer.hasMoreTokens() ) {
				tokens.add( tokenizer.nextToken() );
			}
			return tokens;
		}
	}

	public static Class classForName(String className, ServiceRegistry serviceRegistry) {
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		try {
			return classLoaderService.classForName( className );
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( "Could not find class: " + className );
		}
	}
}
