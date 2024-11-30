/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public class EntityWithCompositeKey {

	@EmbeddedId
	public CompositeKey pk;

	public EntityWithCompositeKey() {
	}

	public EntityWithCompositeKey(CompositeKey pk) {
		this.pk = pk;
	}

}
