/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.polymorphism;

import jakarta.persistence.Entity;


/**
 * @author Guillaume Smet
 */
@Entity
public class CachedItem2 extends Cacheable {

	public CachedItem2() {
		super();
	}

	public CachedItem2(long id, String name) {
		super( id, name );
	}

}
