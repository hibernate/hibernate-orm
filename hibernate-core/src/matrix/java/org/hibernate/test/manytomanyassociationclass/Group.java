/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.manytomanyassociationclass;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gail Badner
 */
public class Group {
	private Long id;
	private String name;
	private Set memberships = new HashSet();

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

	public Set getMemberships() {
		return memberships;
	}

	public void setMemberships(Set memberships) {
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
