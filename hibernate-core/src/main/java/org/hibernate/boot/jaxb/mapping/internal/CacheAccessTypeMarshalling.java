/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.cache.spi.access.AccessType;

/**
 * JAXB marshalling for Hibernate's {@link AccessType}
 *
 * @author Steve Ebersole
 */
public class CacheAccessTypeMarshalling {
	public static AccessType fromXml(String name) {
		return name == null ? null : AccessType.valueOf( name );
	}

	public static String toXml(AccessType accessType) {
		return accessType == null ? null : accessType.name();
	}
}
