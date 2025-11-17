/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;
import jakarta.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
public class IncreaseListener {
	@PreUpdate
	public void increate(CommunicationSystem object) {
		object.communication++;
		object.isFirst = false;
		object.isLast = false;
	}
}
