/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
