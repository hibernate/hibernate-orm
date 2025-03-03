/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantuuid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
public class Account {

	@Id @GeneratedValue Long id;

	@TenantId UUID tenantId;

	@ManyToOne(optional = false)
	Client client;

	public Account(Client client) {
		this.client = client;
	}

	Account() {}
}
