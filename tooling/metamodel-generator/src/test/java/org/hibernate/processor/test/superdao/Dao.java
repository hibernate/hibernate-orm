/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.superdao;

import org.hibernate.annotations.processing.Find;

public interface Dao extends SuperDao {
	@Find
	Book get(String isbn);
}
