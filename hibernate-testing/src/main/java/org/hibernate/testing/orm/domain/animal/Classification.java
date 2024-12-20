/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.animal;

/**
 * Mimic a JDK 5 enum.
 *
 * @author Steve Ebersole
 */
public enum Classification implements Comparable<Classification> {
	COOL,
	LAME;

	public static Classification valueOf(Integer ordinal) {
		if ( ordinal == null ) {
			return null;
		}
		switch ( ordinal ) {
			case 0: return COOL;
			case 1: return LAME;
			default: throw new IllegalArgumentException( "unknown classification ordinal [" + ordinal + "]" );
		}
	}
}
