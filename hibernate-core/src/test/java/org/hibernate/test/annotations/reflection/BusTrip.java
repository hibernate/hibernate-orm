/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.reflection;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class BusTrip {
	private BusTripPk id;
	private Availability status;
	private byte[] serial;
	private Date terminusTime;
	private Map<String, SocialSecurityPhysicalAccount> players;
	private List roads;
	private List<Organization> organizations;

	@EmbeddedId
	public BusTripPk getId() {
		return id;
	}

	public void setId(BusTripPk id) {
		this.id = id;
	}

	public Availability getStatus() {
		return status;
	}

	public void setStatus(Availability status) {
		this.status = status;
	}

	public byte[] getSerial() {
		return serial;
	}

	public void setSerial(byte[] serial) {
		this.serial = serial;
	}

	public Date getTerminusTime() {
		return terminusTime;
	}

	public void setTerminusTime(Date terminusTime) {
		this.terminusTime = terminusTime;
	}

	public List<Organization> getOrganizations() {
		return organizations;
	}

	public void setOrganization(List<Organization> organizations) {
		this.organizations = organizations;
	}
}
