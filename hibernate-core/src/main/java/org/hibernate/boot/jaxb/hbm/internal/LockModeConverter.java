/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
