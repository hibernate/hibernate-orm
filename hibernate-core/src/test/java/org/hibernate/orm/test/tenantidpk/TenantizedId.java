/*
 * SPDX-License-Identifier: Apache-2.0
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
