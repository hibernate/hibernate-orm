//$Id$
package org.hibernate.jpa.test.association;
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
	String id;

	@OneToOne(cascade = CascadeType.ALL)
	IncidentStatus incidentStatus;

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
