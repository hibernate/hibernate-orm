/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.hbm;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Inheritance( strategy = InheritanceType.JOINED )
@Table( name = "ENTITYA" )
public class AImpl implements A {
	private static final long serialVersionUID = 1L;

	private Integer aId = 0;
	private String description;

	public AImpl() {
	}

	@Id
	@GeneratedValue
	@Column( name = "aID" )
	public Integer getAId() {
		return this.aId;
	}

	public void setAId(Integer aId) {
		this.aId = aId;
	}

	@Column( name = "description" )
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
