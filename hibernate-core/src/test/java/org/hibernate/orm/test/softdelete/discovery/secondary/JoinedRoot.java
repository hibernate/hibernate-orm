/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.discovery.secondary;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * @implNote Uses YesNoConverter to work across all databases, even those
 * not supporting an actual BOOLEAN datatype
 *
 * @author Steve Ebersole
 */
@Table(name = "joined_root")
//tag::example-soft-delete-secondary[]
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@SoftDelete(columnName = "removed", converter = YesNoConverter.class)
public abstract class JoinedRoot {
	// ...
//end::example-soft-delete-secondary[]
	@Id
	private Integer id;
	@Basic
	private String name;

	protected JoinedRoot() {
		// for Hibernate use
	}

	public JoinedRoot(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
//tag::example-soft-delete-secondary[]
}
//end::example-soft-delete-secondary[]
