/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.access;

import org.hibernate.HibernateException;

/**
 * Indicates that an unknown AccessType external name was encountered
 * or that an AccessType was requested that the underlying cache provider
 * does not support.
 *
 * @author Steve Ebersole
 *
 * @see AccessType#fromExternalName(String)
 */
public class UnknownAccessTypeException extends HibernateException {
	/**
	 * Constructs the UnknownAccessTypeException.
	 *
	 * @param accessTypeName The external name that could not be resolved.
	 */
	public UnknownAccessTypeException(String accessTypeName) {
		super( "Unknown access type [" + accessTypeName + "]" );
	}
}
