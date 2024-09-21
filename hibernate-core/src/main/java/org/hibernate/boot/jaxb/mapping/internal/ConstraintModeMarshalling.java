/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.ConstraintMode;

/**
 * JAXB marshalling for JPA's {@link ConstraintMode}
 *
 * @author Steve Ebersole
 */
public class ConstraintModeMarshalling {
	public static ConstraintMode fromXml(String name) {
		return name == null ? null : ConstraintMode.valueOf( name );
	}

	public static String toXml(ConstraintMode constraintMode) {
		return constraintMode == null ? null : constraintMode.name();
	}
}
