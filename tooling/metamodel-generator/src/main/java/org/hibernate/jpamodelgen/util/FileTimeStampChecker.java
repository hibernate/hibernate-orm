/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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


