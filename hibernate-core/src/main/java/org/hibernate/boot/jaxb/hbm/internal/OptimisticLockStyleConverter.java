/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
