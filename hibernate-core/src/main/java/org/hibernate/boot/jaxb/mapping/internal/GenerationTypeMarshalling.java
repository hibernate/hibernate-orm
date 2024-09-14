/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
