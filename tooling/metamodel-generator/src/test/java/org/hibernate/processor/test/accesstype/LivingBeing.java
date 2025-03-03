/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Access;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
@Access(jakarta.persistence.AccessType.FIELD)
public class LivingBeing {
	boolean isReallyAlive;

	public boolean isReallyAlive() {
		return isReallyAlive;
	}

	public void setReallyAlive(boolean reallyAlive) {
		isReallyAlive = reallyAlive;
	}

	public int nonPersistent() {
		return 0;
	}
}
