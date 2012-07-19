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
