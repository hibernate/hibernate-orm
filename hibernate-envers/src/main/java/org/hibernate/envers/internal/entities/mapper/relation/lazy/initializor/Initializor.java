/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface Initializor<T> {
	T initialize();
}
