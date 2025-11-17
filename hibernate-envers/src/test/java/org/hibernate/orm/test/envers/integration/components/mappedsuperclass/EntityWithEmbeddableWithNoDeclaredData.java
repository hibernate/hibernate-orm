/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;


import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Jakob Braeuchi.
 */
@Entity
@Table(name="EntEmbWNoDeclData")
@Access(AccessType.FIELD)
@Audited
public class EntityWithEmbeddableWithNoDeclaredData {

	@Id
	@GeneratedValue
	private long id;

	@Column(name = "NAME", length = 100)
	private String name;

	@Embedded
	private EmbeddableWithNoDeclaredData value;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EmbeddableWithNoDeclaredData getValue() {
		return value;
	}

	public void setValue(EmbeddableWithNoDeclaredData value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) ( id ^ ( id >>> 32 ) );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		EntityWithEmbeddableWithNoDeclaredData other = (EntityWithEmbeddableWithNoDeclaredData) obj;
		if ( id != other.id ) {
			return false;
		}
		return true;
	}
}
