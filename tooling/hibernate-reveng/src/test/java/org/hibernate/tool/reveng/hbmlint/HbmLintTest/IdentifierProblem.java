/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbmlint.HbmLintTest;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class IdentifierProblem {

	@Id
	private long name;

	private String id;
}
