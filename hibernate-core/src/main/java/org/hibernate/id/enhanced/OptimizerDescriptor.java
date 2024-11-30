/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

/**
 * @author Gavin King
 */
public interface OptimizerDescriptor {
	boolean isPooled();
	String getExternalName();
	Class<? extends Optimizer> getOptimizerClass()
			throws ClassNotFoundException;
}
