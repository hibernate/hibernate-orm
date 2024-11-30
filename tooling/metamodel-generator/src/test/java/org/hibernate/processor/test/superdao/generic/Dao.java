/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.superdao.generic;

import org.hibernate.annotations.processing.Find;

public interface Dao extends SuperDao<Book,String> {
	@Find
	Book getConc(String isbn);
}
