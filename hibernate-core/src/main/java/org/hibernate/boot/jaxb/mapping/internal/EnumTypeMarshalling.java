/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.EnumType;

/**
 * JAXB marshalling for {@link EnumType}
 *
 * @author Steve Ebersole
 */
public class EnumTypeMarshalling {
	public static EnumType fromXml(String name) {
		return name == null ? null : EnumType.valueOf( name );
	}

	public static String toXml(EnumType enumType) {
		return enumType == null ? null : enumType.name();
	}
}
