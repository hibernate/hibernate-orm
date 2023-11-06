/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import java.util.Locale;

import org.hibernate.annotations.UuidGenerator;

/**
 * JAXB marshalling for {@link UuidGenerator.Style}
 */
public class UuidGeneratorStyleMarshalling {
	public static UuidGenerator.Style fromXml(String name) {
		return name == null ? null : UuidGenerator.Style.valueOf( name.toUpperCase( Locale.ROOT ) );
	}

	public static String toXml(UuidGenerator.Style style) {
		return style == null ? null : style.name().toLowerCase( Locale.ROOT );
	}
}
