/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.embeddables.withcustomenumdef;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class Location {
	public static enum Type {
		POSTAL_CODE,
		COMMUNE,
		REGION,
		PROVINCE,
		COUNTY
	}

	private String name;

	@Enumerated(EnumType.STRING)
//	@Column(columnDefinition = "VARCHAR(32)")
	@Column(name = "`type`")
	private Type type;

	public Location() {
	}

	public Location(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (! obj.getClass().equals( Location.class )) {
			return false;
		}

		Location loc = (Location) obj;
		if (name != null ? !name.equals(loc.name) : loc.name != null) return false;
		if (type != null ? !type.equals(loc.type) : loc.type != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		result = (name != null ? name.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}
}
