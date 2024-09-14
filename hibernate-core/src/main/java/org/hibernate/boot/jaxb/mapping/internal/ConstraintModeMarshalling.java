/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.ConstraintMode;

/**
 * JAXB marshalling for JPA's {@link ConstraintMode}
 *
 * @author Steve Ebersole
 */
public class ConstraintModeMarshalling {
	public static ConstraintMode fromXml(String name) {
		return name == null ? null : ConstraintMode.valueOf( name );
	}

	public static String toXml(ConstraintMode constraintMode) {
		return constraintMode == null ? null : constraintMode.name();
	}
}
