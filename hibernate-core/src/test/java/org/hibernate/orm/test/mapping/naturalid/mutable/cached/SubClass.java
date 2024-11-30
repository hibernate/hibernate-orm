/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.mutable.cached;

import jakarta.persistence.Entity;

/**
 * @author Guenther Demetz
 */
@Entity
public class SubClass extends AllCached {

	public SubClass() {
		super();
	}

	public SubClass(String name) {
		super(name);
	}

}
