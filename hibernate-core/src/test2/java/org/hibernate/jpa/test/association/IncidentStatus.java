/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.association;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

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
