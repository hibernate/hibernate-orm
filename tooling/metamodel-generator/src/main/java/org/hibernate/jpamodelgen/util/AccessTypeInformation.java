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


