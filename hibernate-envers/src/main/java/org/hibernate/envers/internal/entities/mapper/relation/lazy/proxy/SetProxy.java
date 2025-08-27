/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy;

import java.util.Set;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SetProxy<U> extends CollectionProxy<U, Set<U>> implements Set<U> {
	private static final long serialVersionUID = 131464133074137701L;

	public SetProxy() {
	}

	public SetProxy(org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor<Set<U>> initializor) {
		super( initializor );
	}
}
