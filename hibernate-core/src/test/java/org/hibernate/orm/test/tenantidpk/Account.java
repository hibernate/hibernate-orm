/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantidpk;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity @IdClass(TenantizedId.class)
public class Account {

	@Id @GeneratedValue Long id;

	@Id @TenantId UUID tenantId;

	@ManyToOne(optional = false)
	Client client;

	public Account(Client client) {
		this.client = client;
	}

	Account() {}
}
