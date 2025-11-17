/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.internal.util.ReflectHelper;

/**
 * @author Gavin King
 */
public class CustomOptimizerDescriptor implements OptimizerDescriptor {
	private final String className;

	CustomOptimizerDescriptor(String className) {
		this.className = className;
	}
	@Override
	public boolean isPooled() {
		return false;
	}

	@Override
	public String getExternalName() {
		return className;
	}

	@Override @SuppressWarnings("unchecked")
	public Class<? extends Optimizer> getOptimizerClass() throws ClassNotFoundException {
		return (Class<? extends Optimizer>) ReflectHelper.classForName( className );
	}
}
