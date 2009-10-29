package org.hibernate.ejb.test.metadata;

import javax.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Cattish extends Feline {
	private long hoursOfSleep;

	public long getHoursOfSleep() {
		return hoursOfSleep;
	}

	public void setHoursOfSleep(long hoursOfSleep) {
		this.hoursOfSleep = hoursOfSleep;
	}
}
