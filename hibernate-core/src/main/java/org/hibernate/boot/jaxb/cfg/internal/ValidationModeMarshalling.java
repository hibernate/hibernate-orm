/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.cfg.internal;

import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.ValidationMode;

/**
 * @author Steve Ebersole
 */
public class ValidationModeMarshalling {
	public static ValidationMode fromXml(String name) {
		if ( StringHelper.isEmpty( name ) ) {
			return ValidationMode.AUTO;
		}
		return ValidationMode.valueOf( name );
	}

	public static String toXml(ValidationMode validationMode) {
		if ( validationMode == null ) {
			return null;
		}
		return validationMode.name();
	}
}
