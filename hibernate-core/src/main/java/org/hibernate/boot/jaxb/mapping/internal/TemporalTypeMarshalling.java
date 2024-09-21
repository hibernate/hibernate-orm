/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.TemporalType;

/**
 * JAXB marshalling for {@link TemporalType}
 *
 * @author Steve Ebersole
 */
public class TemporalTypeMarshalling {
	public static TemporalType fromXml(String name) {
		return name == null ? null : TemporalType.valueOf( name );
	}

	public static String toXml(TemporalType temporalType) {
		return temporalType == null ? null : temporalType.name();
	}
}
