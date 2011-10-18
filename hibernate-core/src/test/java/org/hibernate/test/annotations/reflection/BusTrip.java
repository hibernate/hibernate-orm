//$Id$
package org.hibernate.test.annotations.reflection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

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
}
