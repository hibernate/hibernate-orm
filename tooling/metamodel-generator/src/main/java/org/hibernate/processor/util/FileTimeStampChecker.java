/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

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
	public boolean equals(@Nullable Object o) {
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
