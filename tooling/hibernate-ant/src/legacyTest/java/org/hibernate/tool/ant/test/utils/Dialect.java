/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.test.utils;

import org.hibernate.dialect.DatabaseVersion;

public class Dialect extends org.hibernate.dialect.Dialect {
	public Dialect() {
		super((DatabaseVersion)null);
	}
}
