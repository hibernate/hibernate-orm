/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

/**
 * @author Steve Ebersole
 */
public class EverythingSelector implements EnhancementSelector {
	/**
	 * Singleton access
	 */
	public static final EverythingSelector INSTANCE = new EverythingSelector();

	@Override
	public boolean select(String name) {
		return true;
	}
}
