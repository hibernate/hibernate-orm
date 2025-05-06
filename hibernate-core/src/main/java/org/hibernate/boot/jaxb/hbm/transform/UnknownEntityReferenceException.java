/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.Origin;

/**
 * We encountered a reference to an entity by name which we cannot resolve
 *
 * @author Steve Ebersole
 */
public class UnknownEntityReferenceException extends MappingException {
	public UnknownEntityReferenceException(String name, Origin origin) {
		super( String.format(
				Locale.ROOT,
				"Could not resolve entity name `%s` : %s (%s)",
				name,
				origin.getName(),
				origin.getType()
		) );
	}
}
