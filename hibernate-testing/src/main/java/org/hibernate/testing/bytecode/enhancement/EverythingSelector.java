/*
 * SPDX-License-Identifier: Apache-2.0
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
