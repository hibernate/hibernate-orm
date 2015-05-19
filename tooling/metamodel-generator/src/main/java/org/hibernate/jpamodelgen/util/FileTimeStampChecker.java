/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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


