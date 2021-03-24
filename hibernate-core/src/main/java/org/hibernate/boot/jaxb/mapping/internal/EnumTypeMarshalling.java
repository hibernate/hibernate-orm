/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import javax.persistence.EnumType;

/**
 * Marshalling support for dealing with JPA EnumType enums.  Plugged into JAXB for binding
 *
 * @author Steve Ebersole
 */
public class EnumTypeMarshalling {
	public static EnumType fromXml(String name) {
		return EnumType.valueOf( name );
	}

	public static String toXml(EnumType enumType) {
		return enumType.name();
	}
}
