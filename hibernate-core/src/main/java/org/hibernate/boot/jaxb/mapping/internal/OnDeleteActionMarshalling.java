/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.annotations.OnDeleteAction;

/**
 * @author Steve Ebersole
 */
public class OnDeleteActionMarshalling {
	public static OnDeleteAction fromXml(String name) {
		return name == null ? null : OnDeleteAction.fromExternalForm( name );
	}

	public static String toXml(OnDeleteAction accessType) {
		return accessType == null ? null : accessType.getAlternativeName();
	}
}
