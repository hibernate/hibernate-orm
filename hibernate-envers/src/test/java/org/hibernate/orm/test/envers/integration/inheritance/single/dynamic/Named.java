/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.single.dynamic;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;

import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

@Entity
@Audited
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@DiscriminatorOptions(insert = false)
public abstract class Named {

	@Id
	private final String name;

	@Column
	@Audited
	private final String type;

	@Column
	@Audited
	private String description;

	protected Named() {
		name = null;
		type = null;
	}

	protected Named(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
