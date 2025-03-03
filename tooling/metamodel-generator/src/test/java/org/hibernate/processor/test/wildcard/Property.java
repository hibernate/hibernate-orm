/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.wildcard;

public interface Property<T> {

	String getName();

	T getValue();
}
