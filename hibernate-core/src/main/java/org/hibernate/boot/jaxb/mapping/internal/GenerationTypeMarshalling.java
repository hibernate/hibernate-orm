/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.GenerationType;

/**
 * JAXB marshalling for {@link GenerationType}
 *
 * @author Steve Ebersole
 */
public class GenerationTypeMarshalling {
	public static GenerationType fromXml(String name) {
		return name == null ? null : GenerationType.valueOf( name );
	}

	public static String toXml(GenerationType generationType) {
		return generationType == null ? null : generationType.name();
	}
}
