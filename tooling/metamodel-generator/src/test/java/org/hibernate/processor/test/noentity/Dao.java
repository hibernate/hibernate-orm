/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.noentity;

import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.processing.HQL;

public interface Dao {
	@HQL("select upper('Hibernate')")
	TypedQuery<String> getName();
}
