/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;
import jakarta.persistence.MappedSuperclass;

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
