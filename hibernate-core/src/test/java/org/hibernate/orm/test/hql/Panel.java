/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Panel implements Serializable {

	@Id
	private Long id;

	private Long clientId;

	private String deltaStamp;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}

	public String getDeltaStamp() {
		return deltaStamp;
	}

	public void setDeltaStamp(String deltaStamp) {
		this.deltaStamp = deltaStamp;
	}
}
