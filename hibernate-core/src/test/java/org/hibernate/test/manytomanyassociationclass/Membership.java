/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
