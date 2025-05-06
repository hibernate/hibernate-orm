/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class TrtPanel implements Serializable {

	@Id
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Panel panel;

	private Long clientId;

	private String deltaStamp;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Panel getPanel() {
		return panel;
	}

	public void setPanel(Panel panel) {
		this.panel = panel;
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
