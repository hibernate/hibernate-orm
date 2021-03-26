/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import javax.persistence.GenerationType;

/**
 * Marshalling support for dealing with JPA GenerationType enums.  Plugged into JAXB for binding
 *
 * @author Steve Ebersole
 */
public class GenerationTypeMarshalling {
	public static GenerationType fromXml(String name) {
		return GenerationType.valueOf( name );
	}

	public static String toXml(GenerationType accessType) {
		return accessType.name();
	}
}
