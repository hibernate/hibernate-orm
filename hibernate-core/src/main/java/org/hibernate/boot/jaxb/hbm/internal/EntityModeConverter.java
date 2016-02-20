/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.internal;

import org.hibernate.EntityMode;

/**
 * @author Steve Ebersole
 */
public class EntityModeConverter {
	public static EntityMode fromXml(String name) {
		return EntityMode.parse( name );
	}

	public static String toXml(EntityMode entityMode) {
		return ( null == entityMode ) ? null : entityMode.getExternalName();
	}
}
