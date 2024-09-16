/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.internal;

import java.util.Locale;

import org.hibernate.tuple.GenerationTiming;

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
