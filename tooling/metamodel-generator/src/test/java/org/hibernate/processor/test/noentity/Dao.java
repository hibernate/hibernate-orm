/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.noentity;

import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.processing.HQL;

public interface Dao {
	@HQL("select upper('Hibernate')")
	TypedQuery<String> getName();
}
