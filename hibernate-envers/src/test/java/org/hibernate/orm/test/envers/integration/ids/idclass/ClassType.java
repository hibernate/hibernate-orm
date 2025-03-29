/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.idclass;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Audited
@Entity
public class ClassType implements Serializable {
	@Id
	@Column(name = "Name")
	private String type;

	private String description;

	public ClassType() {
	}

	public ClassType(String type, String description) {
		this.type = type;
		this.description = description;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ClassType) ) {
			return false;
		}

		ClassType classType = (ClassType) o;

		if ( type != null ? !type.equals( classType.type ) : classType.type != null ) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return "ClassType(type = " + type + ", description = " + description + ")";
	}

	@Override
	public int hashCode() {
		return type != null ? type.hashCode() : 0;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
