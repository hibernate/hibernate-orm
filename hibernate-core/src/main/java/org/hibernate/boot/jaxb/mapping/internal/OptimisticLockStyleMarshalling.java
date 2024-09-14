/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;


import org.hibernate.engine.OptimisticLockStyle;

/**
 * JAXB marshalling for {@link OptimisticLockStyle}
 *
 * @author Steve Ebersole
 */
public class OptimisticLockStyleMarshalling {
	public static OptimisticLockStyle fromXml(String name) {
		return name == null ? null : OptimisticLockStyle.valueOf( name );
	}

	public static String toXml(OptimisticLockStyle lockMode) {
		return lockMode == null ? null : lockMode.name();
	}
}
