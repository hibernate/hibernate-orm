/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.discovery.override;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.orm.test.softdelete.CustomTrueFalseConverter;

import java.util.Collection;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "the_table")
@SoftDelete(converter = CustomTrueFalseConverter.class)
public class AnEntity {
	@Id
	private Integer id;
	@Basic
	private String name;
	@ElementCollection
	@CollectionTable(name="elements", joinColumns = @JoinColumn(name = "owner_fk"))
	@Column(name="txt")
	@SoftDelete(converter = CustomTrueFalseConverter.class)
	private Collection<String> elements;

	protected AnEntity() {
		// for Hibernate use
	}

	public AnEntity(Integer id, String name) {
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
}
