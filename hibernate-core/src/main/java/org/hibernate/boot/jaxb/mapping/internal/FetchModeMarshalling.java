/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.annotations.FetchMode;

/**
 * @author Steve Ebersole
 */
public class FetchModeMarshalling {
	public static FetchMode fromXml(String name) {
		return name == null ? null : FetchMode.valueOf( name );
	}

	public static String toXml(FetchMode fetchType) {
		return fetchType == null ? null : fetchType.name();
	}
}
