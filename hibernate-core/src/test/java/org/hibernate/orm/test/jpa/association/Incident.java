/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.association;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

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
