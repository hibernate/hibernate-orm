/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.converter.internal;

/**
 * @author Gavin King
 */
public class EnumHelper {
	public static String[] getEnumeratedValues(Class<? extends Enum<?>> enumClass) {
		Enum<?>[] values = enumClass.getEnumConstants();
		String[] names = new String[values.length];
		for ( int i = 0; i < values.length; i++ ) {
			names[i] = values[i].name();
		}
		return names;
	}
}
