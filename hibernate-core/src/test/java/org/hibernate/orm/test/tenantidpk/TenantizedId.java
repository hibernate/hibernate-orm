/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantidpk;

import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
public class TenantizedId {
	Long id;
	UUID tenantId;

	public TenantizedId(Long id, UUID tenantId) {
		this.id = id;
		this.tenantId = tenantId;
	}

	TenantizedId() {}
}
