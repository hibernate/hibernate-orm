/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.internal;

import org.hibernate.LockMode;

/**
 * @author Steve Ebersole
 */
public class LockModeConverter {
	public static LockMode fromXml(String name) {
		return LockMode.fromExternalForm( name );
	}

	public static String toXml(LockMode lockMode) {
		return lockMode.toExternalForm();
	}
}
