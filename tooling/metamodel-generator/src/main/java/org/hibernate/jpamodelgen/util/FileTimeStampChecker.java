/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.jpamodelgen.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Hardy Ferentschik
 */
public class FileTimeStampChecker implements Serializable {

	private Map<String, Long> lastModifiedCache;

	public FileTimeStampChecker() {
		lastModifiedCache = new HashMap<String, Long>();
	}

	public void add(String fileName, Long lastModified) {
		lastModifiedCache.put( fileName, lastModified );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		FileTimeStampChecker that = (FileTimeStampChecker) o;

		if ( !lastModifiedCache.equals( that.lastModifiedCache ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return lastModifiedCache.hashCode();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "FileTimeStampChecker" );
		sb.append( "{lastModifiedCache=" ).append( lastModifiedCache );
		sb.append( '}' );
		return sb.toString();
	}
}


