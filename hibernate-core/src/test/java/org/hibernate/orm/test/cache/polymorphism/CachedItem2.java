/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
