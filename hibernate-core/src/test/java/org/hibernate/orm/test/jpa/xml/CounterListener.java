/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
public class CounterListener {
	public static int insert;
	public static int update;
	public static int delete;

	@PrePersist
	public void increaseInsert(Object object) {
		insert++;
	}

	@PreUpdate
	public void increaseUpdate(Object object) {
		update++;
	}

	@PreRemove
	public void increaseDelete(Object object) {
		delete++;
	}

	public static void reset() {
		insert = 0;
		update = 0;
		delete = 0;
	}
}
