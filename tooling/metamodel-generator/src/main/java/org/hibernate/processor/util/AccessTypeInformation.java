/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import jakarta.persistence.AccessType;
import org.jspecify.annotations.Nullable;

/**
 * Encapsulates the access type information for a single class.
 *
 * @author Hardy Ferentschik
 */
public class AccessTypeInformation {
	private final String fullyQualifiedName;

	/**
	 * Access type explicitly specified in xml or on an entity.
	 */
	private @Nullable AccessType explicitAccessType;

	/**
	 * The default type for en entity. This type might change during the parsing/discovering process. The reason
	 * for that is the ability to mix and match xml and annotation configuration.
	 */
	private @Nullable AccessType defaultAccessType;

	static final AccessType DEFAULT_ACCESS_TYPE = AccessType.FIELD;

	public AccessTypeInformation(
			String fullyQualifiedName,
			@Nullable AccessType explicitAccessType,
			@Nullable AccessType defaultAccessType) {
		this.fullyQualifiedName = fullyQualifiedName;
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

	public @Nullable AccessType getDefaultAccessType() {
		return defaultAccessType;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AccessTypeInformation" );
		sb.append( "{fqcn='" ).append(fullyQualifiedName).append( '\'' );
		sb.append( ", explicitAccessType=" ).append( explicitAccessType );
		sb.append( ", defaultAccessType=" ).append( defaultAccessType );
		sb.append( '}' );
		return sb.toString();
	}
}
