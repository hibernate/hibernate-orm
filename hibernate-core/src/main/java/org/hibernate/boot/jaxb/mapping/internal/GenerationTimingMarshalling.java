/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.boot.jaxb.mapping.GenerationTiming ;

/**
 * JAXB marshalling for {@link GenerationTiming}
 *
 * @author Steve Ebersole
 */
public class GenerationTimingMarshalling {
	public static GenerationTiming fromXml(String name) {
		return name == null ? null : GenerationTiming.valueOf( name );
	}

	public static String toXml(GenerationTiming generationTiming) {
		return null == generationTiming ? null : generationTiming.name();
	}
}
