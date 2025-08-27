/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.models.spi.ClassDetails;

/**
 * Indicates that {@link jakarta.persistence.AccessType} could not be
 * determined
 *
 * @author Steve Ebersole
 */
public class AccessTypeDeterminationException extends MappingException {
	public AccessTypeDeterminationException(ClassDetails managedClass) {
		super(
				String.format(
						Locale.ROOT,
						"Unable to determine default `AccessType` for class `%s`",
						managedClass.getName()
				)
		);
	}
}
