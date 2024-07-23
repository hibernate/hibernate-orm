/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
