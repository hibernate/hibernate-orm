/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Account {

	@Id @GeneratedValue Long id;

	@TenantId String tenantId;

	@ManyToOne(optional = false) Client client;

	public Account(Client client) {
		this.client = client;
	}

	Account() {}
}
