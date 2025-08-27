/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.FetchType;

/**
 * JAXB marshalling for {@link FetchType}
 *
 * @author Steve Ebersole
 */
public class FetchTypeMarshalling {
	public static FetchType fromXml(String name) {
		return name == null ? null : FetchType.valueOf( name );
	}

	public static String toXml(FetchType fetchType) {
		return fetchType == null ? null : fetchType.name();
	}
}
