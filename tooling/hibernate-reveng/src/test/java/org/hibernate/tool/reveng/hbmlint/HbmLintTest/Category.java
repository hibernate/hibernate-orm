/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbmlint.HbmLintTest;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.Set;

@Entity
public class Category {

	@Id
	@GeneratedValue
	private long id;

	@OneToMany
	@JoinColumn(name = "PARENT_ID")
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	private Set<Category> childCategories;
}
