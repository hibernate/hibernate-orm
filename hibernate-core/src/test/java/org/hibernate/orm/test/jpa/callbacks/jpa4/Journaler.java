/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.EntityListener;
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostUpdate;

/**
 * @author Steve Ebersole
 */
@EntityListener
public class Journaler {
	public static int bookCreateCount;
	public static int bookUpdateCount;
	public static int bookDeleteCount;

	public static void reset() {
		bookCreateCount = 0;
		bookUpdateCount = 0;
		bookDeleteCount = 0;
	}

	@PostInsert
	public void afterCreation(Book book) {
		bookCreateCount++;
	}

	@PostUpdate
	public void afterUpdate(Book book) {
		bookUpdateCount++;
	}

	@PostDelete
	public void afterDelete(Book book) {
		bookDeleteCount++;
	}
}
