/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import org.hibernate.MappingException;

/**
 * Indicates a problem resolving a member from {@linkplain org.hibernate.models.spi.ClassDetails}
 *
 * @author Steve Ebersole
 */
public class MemberResolutionException extends MappingException {
	public MemberResolutionException(String message) {
		super( message );
	}

	public MemberResolutionException(String message, Throwable cause) {
		super( message, cause );
	}
}
