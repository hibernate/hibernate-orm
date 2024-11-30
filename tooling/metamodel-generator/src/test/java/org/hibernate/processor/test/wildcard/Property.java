/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.wildcard;

public interface Property<T> {

	String getName();

	T getValue();
}
