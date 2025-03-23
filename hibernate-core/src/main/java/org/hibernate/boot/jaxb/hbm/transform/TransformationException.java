/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;

/**
 * Generalized error while performing {@code hbm.xml} transformation
 *
 * @author Steve Ebersole
 */
public class TransformationException extends MappingException {
	public TransformationException(String message, Origin origin) {
		super( message, origin );
	}

	public TransformationException(String message, Throwable root, Origin origin) {
		super( message, root, origin );
	}
}
