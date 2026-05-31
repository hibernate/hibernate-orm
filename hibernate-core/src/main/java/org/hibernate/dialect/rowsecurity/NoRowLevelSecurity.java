/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

/**
 * No-op row-level security support.
 *
 * @author Gavin King
 */
public class NoRowLevelSecurity implements RowLevelSecurity {
	public static final NoRowLevelSecurity INSTANCE = new NoRowLevelSecurity();

	@Override
	public boolean supportsRowLevelSecurity() {
		return false;
	}
}
