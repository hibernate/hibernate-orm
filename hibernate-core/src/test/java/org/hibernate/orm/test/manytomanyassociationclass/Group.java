/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomanyassociationclass;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gail Badner
 */
public class Group {
	private Long id;
	private String name;
	private Set<Membership> memberships = new HashSet<>();

	public Group() {
	}

	public Group(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Membership> getMemberships() {
		return memberships;
	}

	public void setMemberships(Set<Membership> memberships) {
		this.memberships = memberships;
	}

	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj instanceof  Group ) {
			Group grp = ( Group ) obj;
			if ( grp.getName() != null && name != null ) {
				return grp.getName().equals( name );
			}
			else {
				return super.equals( obj );
			}
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return ( name == null ? super.hashCode() : name.hashCode() );
	}
}
