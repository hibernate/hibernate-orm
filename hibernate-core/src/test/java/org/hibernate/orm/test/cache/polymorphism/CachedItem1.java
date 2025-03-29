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
public class CachedItem1 extends Cacheable {

	public CachedItem1() {
		super();
	}

	public CachedItem1(long id, String name) {
		super( id, name );
	}

}
