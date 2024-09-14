/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.annotations.NotFoundAction;

/**
 * @author Steve Ebersole
 */
public class NotFoundActionMarshalling {
	public static NotFoundAction fromXml(String name) {
		return name == null ? null : NotFoundAction.valueOf( name );
	}

	public static String toXml(NotFoundAction action) {
		return action == null ? null : action.name();
	}
}
