/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.superdao;

import org.hibernate.annotations.processing.Find;

public interface Dao extends SuperDao {
	@Find
	Book get(String isbn);
}
