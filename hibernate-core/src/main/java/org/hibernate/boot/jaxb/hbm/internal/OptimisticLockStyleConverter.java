/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.internal;

import java.util.Locale;

import org.hibernate.engine.OptimisticLockStyle;

/**
 * Handles conversion to/from Hibernate's OptimisticLockStyle enum during
 * JAXB processing.
 *
 * @author Steve Ebersole
 */
public class OptimisticLockStyleConverter {
	public static OptimisticLockStyle fromXml(String name) {
		return OptimisticLockStyle.valueOf( name == null ? null : name.toUpperCase( Locale.ENGLISH ) );
	}

	public static String toXml(OptimisticLockStyle lockMode) {
		return lockMode == null ? null : lockMode.name().toLowerCase( Locale.ENGLISH );
	}
}
