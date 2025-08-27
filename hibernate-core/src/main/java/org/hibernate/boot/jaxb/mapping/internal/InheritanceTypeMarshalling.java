/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.InheritanceType;

/**
 * JAXB marshalling for {@link InheritanceType}
 *
 * @author Steve Ebersole
 */
public class InheritanceTypeMarshalling {
	public static InheritanceType fromXml(String name) {
		return name == null ? null : InheritanceType.valueOf( name );
	}

	public static String toXml(InheritanceType inheritanceType) {
		return inheritanceType == null ? null : inheritanceType.name();
	}
}
