/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
