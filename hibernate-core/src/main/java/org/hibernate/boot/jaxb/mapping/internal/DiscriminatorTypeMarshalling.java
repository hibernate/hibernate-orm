/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.DiscriminatorType;

/**
 * JAXB marshalling for {@link DiscriminatorType}
 *
 * @author Steve Ebersole
 */
public class DiscriminatorTypeMarshalling {
	public static DiscriminatorType fromXml(String name) {
		return name == null ? null : DiscriminatorType.valueOf( name );
	}

	public static String toXml(DiscriminatorType discriminatorType) {
		return discriminatorType == null ? null : discriminatorType.name();
	}
}
