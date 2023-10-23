/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.EnumType;

/**
 * JAXB marshalling for {@link EnumType}
 *
 * @author Steve Ebersole
 */
public class EnumTypeMarshalling {
	public static EnumType fromXml(String name) {
		return name == null ? null : EnumType.valueOf( name );
	}

	public static String toXml(EnumType enumType) {
		return enumType == null ? null : enumType.name();
	}
}
