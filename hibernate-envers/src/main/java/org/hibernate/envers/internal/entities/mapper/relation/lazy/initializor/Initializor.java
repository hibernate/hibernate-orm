/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface Initializor<T> {
	T initialize();
}
