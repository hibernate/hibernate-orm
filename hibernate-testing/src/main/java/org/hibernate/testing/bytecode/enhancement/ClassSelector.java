/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

/**
 * EnhancementSelector based on class name
 *
 * @author Steve Ebersole
 */
public class ClassSelector implements EnhancementSelector {
	private final String className;

	public ClassSelector(String className) {
		this.className = className;
	}

	@Override
	public boolean select(String name) {
		return name.equals( className );
	}
}
