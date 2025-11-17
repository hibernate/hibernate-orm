/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.association;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class IncidentStatus {
	@Id
	String id;

	@OneToOne(mappedBy = "incidentStatus")
	Incident incident;

	public IncidentStatus() {
	}

	public IncidentStatus(String id) {
		this.id = id;
	}

	public Incident getIncident() {
		return incident;
	}

	public void setIncident(Incident incident) {
		this.incident = incident;
	}

	@Override
	public String toString() {
		return "IncidentStatus " + id;
	}
}
