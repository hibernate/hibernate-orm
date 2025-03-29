/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.superdao.generic;

import org.hibernate.annotations.processing.Find;

public interface Dao extends SuperDao<Book,String> {
	@Find
	Book getConc(String isbn);
}
