/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.internal;

import java.util.Locale;

import org.hibernate.boot.jaxb.mapping.GenerationTiming;

/**
 * @author Steve Ebersole
 */
public class GenerationTimingConverter {
	public static GenerationTiming fromXml(String name) {
		return GenerationTiming.parseFromName( name );
	}

	public static String toXml(GenerationTiming generationTiming) {
		return ( null == generationTiming ) ?
				null :
				generationTiming.name().toLowerCase( Locale.ENGLISH );
	}
}
