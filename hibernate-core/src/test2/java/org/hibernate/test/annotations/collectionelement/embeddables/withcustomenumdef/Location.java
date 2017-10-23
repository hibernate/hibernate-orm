/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement.embeddables.withcustomenumdef;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Column;

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
