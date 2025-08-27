/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.superdao;

import jakarta.data.repository.Repository;
import org.hibernate.annotations.processing.Find;

@Repository
public interface Repo extends SuperRepo {
	@Find
	Book get(String isbn);
}
