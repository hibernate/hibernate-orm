/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.io.Serializable;

/**
 * @deprecated This is a legacy of very ancient versions of Hibernate.
 *
 * @author Gavin King
 */
@Deprecated
public class MarkerObject implements Serializable {
	private final String name;

	public MarkerObject(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
