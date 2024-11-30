/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EntityWithStringCompositeKey {

	private StringCompositeKey pk;

	@EmbeddedId
	public StringCompositeKey getPk() {
		return pk;
	}

	public void setPk(StringCompositeKey pk) {
		this.pk = pk;
	}
}
