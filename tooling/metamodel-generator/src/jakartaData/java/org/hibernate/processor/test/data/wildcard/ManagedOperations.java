/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.wildcard;

public interface ManagedOperations<E> {
	@SuppressWarnings("unchecked")
	default Class<? extends E> getEntityClass() {
		return (Class<? extends E>) Object.class;
	}
}
