/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.association;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Incident {
	@Id
	private String id;

	@OneToOne(cascade = CascadeType.ALL)
	private IncidentStatus incidentStatus;

	public Incident() {
	}

	public Incident(String id) {
		this.id = id;
	}

	public IncidentStatus getIncidentStatus() {
		return incidentStatus;
	}

	public void setIncidentStatus(IncidentStatus incidentStatus) {
		this.incidentStatus = incidentStatus;
	}

	@Override
	public String toString() {
		return "Incident: " + id + " " + incidentStatus;
	}
}
