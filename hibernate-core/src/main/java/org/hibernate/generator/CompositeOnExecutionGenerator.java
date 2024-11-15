/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator;

public interface CompositeOnExecutionGenerator extends OnExecutionGenerator {
	OnExecutionGenerator getPropertyGenerator(String propertyName);
	boolean[] writePropertyValues();

}
