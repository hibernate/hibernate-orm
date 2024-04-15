/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import javax.persistence.ParameterMode;

/**
 * Marshalling support for dealing with JPA ParameterMode enums.  Plugged into JAXB for binding
 *
 * @author Steve Ebersole
 */
public class ParameterModeMarshalling {
	public static ParameterMode fromXml(String name) {
		return ParameterMode.valueOf( name );
	}

	public static String toXml(ParameterMode parameterMode) {
		return parameterMode.name();
	}
}
