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

import java.io.Serializable;

/**
 * Models a user's membership in a group.
 *
 * @author Gail Badner
 */
public class Membership {
	private Serializable id;
	private String name;
	private User user;
	private Group group;

	public Membership() {
	}

	public Membership(Serializable id) {
		this.id = id;
	}

	public Membership(String name) {
		this.name = name;
	}

	public Membership(Serializable id, String name) {
		this.id = id;
		this.name = name;
	}

	public Serializable getId() {
		return id;
	}

	public void setId(Serializable id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj instanceof Membership ) {
			Membership mem = ( Membership ) obj;
			if ( mem.getName() != null && name != null ) {
				return mem.getName().equals( name );
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
