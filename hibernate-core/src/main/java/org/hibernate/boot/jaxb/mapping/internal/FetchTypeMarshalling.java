/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.FetchType;

/**
 * JAXB marshalling for {@link FetchType}
 *
 * @author Steve Ebersole
 */
public class FetchTypeMarshalling {
	public static FetchType fromXml(String name) {
		return name == null ? null : FetchType.valueOf( name );
	}

	public static String toXml(FetchType fetchType) {
		return fetchType == null ? null : fetchType.name();
	}
}
