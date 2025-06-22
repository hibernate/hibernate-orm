/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.internal;

import org.hibernate.metamodel.RepresentationMode;

/**
 * @author Steve Ebersole
 */
public class RepresentationModeConverter {
	public static RepresentationMode fromXml(String name) {
		return RepresentationMode.fromExternalName( name );
	}

	public static String toXml(RepresentationMode entityMode) {
		return ( null == entityMode ) ? null : entityMode.getExternalName();
	}
}
