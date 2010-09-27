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

// $Id$

package org.hibernate.jpamodelgen;

import javax.persistence.AccessType;

/**
 * Encapsulates the access type information for a single class.
 *
 * @author Hardy Ferentschik
 */
public class AccessTypeInformation {
	private final String fqcn;

	/**
	 * Access type explicitly specified in xml or on an entity.
	 */
	private AccessType explicitAccessType;

	/**
	 * The default type for en entity. This type might change during the parsing/discovering process. The reason
	 * for that is the ability to mix and match xml and annotation configuration.
	 */
	private AccessType defaultAccessType;

	private static final AccessType DEFAULT_ACCESS_TYPE = AccessType.PROPERTY;

	public AccessTypeInformation(String fqcn, AccessType explicitAccessType, AccessType defaultAccessType) {
		this.fqcn = fqcn;
		this.explicitAccessType = explicitAccessType;
		this.defaultAccessType = defaultAccessType;
	}

	public boolean isAccessTypeResolved() {
		return explicitAccessType != null || defaultAccessType != null;
	}

	public AccessType getAccessType() {
		if ( explicitAccessType != null ) {
			return explicitAccessType;
		}
		else if ( defaultAccessType != null ) {
			return defaultAccessType;

		}
		else {
			return DEFAULT_ACCESS_TYPE;
		}
	}

	public void setDefaultAccessType(AccessType defaultAccessType) {
		this.defaultAccessType = defaultAccessType;
	}

	public void setExplicitAccessType(AccessType explicitAccessType) {
		this.explicitAccessType = explicitAccessType;
	}

	public AccessType getDefaultAccessType() {
		return defaultAccessType;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AccessTypeInformation" );
		sb.append( "{fqcn='" ).append( fqcn ).append( '\'' );
		sb.append( ", explicitAccessType=" ).append( explicitAccessType );
		sb.append( ", defaultAccessType=" ).append( defaultAccessType );
		sb.append( '}' );
		return sb.toString();
	}
}


