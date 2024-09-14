/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
