/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlmapped;

/**
 * @author Hardy Ferentschik
 */
public class LivingBeing {
	boolean reallyAlive;

	public boolean isReallyAlive() {
		return reallyAlive;
	}

	public void setReallyAlive(boolean reallyAlive) {
		this.reallyAlive = reallyAlive;
	}

	public int nonPersistent() {
		return 0;
	}
}
