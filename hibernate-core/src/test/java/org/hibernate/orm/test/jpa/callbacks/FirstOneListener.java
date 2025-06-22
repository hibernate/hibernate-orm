/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;
import jakarta.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
public class FirstOneListener {
	@PreUpdate
	public void firstOne(CommunicationSystem object) {
		if ( !object.isFirst ) throw new IllegalStateException();
		object.isFirst = true;
		object.isLast = false;
		object.communication++;
	}
}
