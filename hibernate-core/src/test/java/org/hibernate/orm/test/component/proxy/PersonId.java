/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.proxy;

import java.io.Serializable;

@SuppressWarnings("all")
public class PersonId implements Serializable {

	private int id;

	private int clientId;

	public PersonId() {
	}

	public PersonId(int aId, int aClientId) {
		setId( aId );
		setClientId( aClientId );
	}

	public int getId() {
		return id;
	}

	public void setId(int aId) {
		this.id = aId;
	}

	public int getClientId() {

		return clientId;
	}

	public void setClientId(int aClientId) {
		clientId = aClientId;
	}
}
