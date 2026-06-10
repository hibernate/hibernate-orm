/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.lint.LintTest;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class BrokenLazy {

	@Id
	long id;

	public BrokenLazy(long id) {
		this.id = id;
	}
}
