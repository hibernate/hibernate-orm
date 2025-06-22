/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.ParameterMode;

/**
 * JAXB marshalling for {@link ParameterMode}
 *
 * @author Steve Ebersole
 */
public class ParameterModeMarshalling {
	public static ParameterMode fromXml(String name) {
		return name == null ? null : ParameterMode.valueOf( name );
	}

	public static String toXml(ParameterMode parameterMode) {
		return parameterMode == null ? null : parameterMode.name();
	}
}
