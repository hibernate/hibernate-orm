/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Hardy Ferentschik
 */
public final class FileTimeStampChecker implements Serializable {

	private final Map<String, Long> lastModifiedCache = new HashMap<>();

	public void add(String fileName, Long lastModified) {
		lastModifiedCache.put( fileName, lastModified );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof FileTimeStampChecker that ) ) {
			return false;
		}
		else {
			return Objects.equals( this.lastModifiedCache, that.lastModifiedCache );
		}
	}

	@Override
	public int hashCode() {
		return lastModifiedCache.hashCode();
	}

	@Override
	public String toString() {
		return "FileTimeStampChecker{lastModifiedCache=" + lastModifiedCache + '}';
	}
}
