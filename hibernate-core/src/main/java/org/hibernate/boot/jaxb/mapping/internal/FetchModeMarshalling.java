/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.annotations.FetchMode;

/**
 * @author Steve Ebersole
 */
public class FetchModeMarshalling {
	public static FetchMode fromXml(String name) {
		return name == null ? null : FetchMode.valueOf( name );
	}

	public static String toXml(FetchMode fetchType) {
		return fetchType == null ? null : fetchType.name();
	}
}
