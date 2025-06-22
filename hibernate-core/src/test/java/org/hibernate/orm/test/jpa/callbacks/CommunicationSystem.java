/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
@EntityListeners(IncreaseListener.class)
public class CommunicationSystem {
	public int communication = 0;
	public boolean isFirst = true;
	public boolean isLast;

	public void init() {
		communication = 0;
		isFirst = true;
		isLast = false;
	}
}
