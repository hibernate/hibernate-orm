/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.internal;

import org.hibernate.metamodel.RepresentationMode;

/**
 * @author Steve Ebersole
 */
public class RepresentationModeConverter {
	public static RepresentationMode fromXml(String name) {
		return RepresentationMode.fromExternalName( name );
	}

	public static String toXml(RepresentationMode entityMode) {
		return ( null == entityMode ) ? null : entityMode.getExternalName();
	}
}
