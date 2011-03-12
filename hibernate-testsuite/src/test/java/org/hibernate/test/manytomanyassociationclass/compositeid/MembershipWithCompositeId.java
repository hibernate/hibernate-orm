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
package org.hibernate.test.manytomanyassociationclass.compositeid;

import java.io.Serializable;

import org.hibernate.test.manytomanyassociationclass.Group;
import org.hibernate.test.manytomanyassociationclass.Membership;
import org.hibernate.test.manytomanyassociationclass.User;

/**
 * Models a user's membership in a group.
 *
 * @author Gail Badner
 */
public class MembershipWithCompositeId extends Membership {

	public static class Id implements Serializable {
		private Long userId;
		private Long groupId;

		public Id() {
		}

		public Id(Long userId, Long groupId) {
			this.userId = userId;
			this.groupId = groupId;
		}

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getGroupId() {
			return groupId;
		}

		public void setGroupId(Long groupId) {
			this.groupId = groupId;
		}

		public boolean equals(Object o) {
			if ( o != null && o instanceof Id ) {
				Id that = ( Id ) o;
				return this.userId.equals( that.userId ) &&
						this.groupId.equals( that.groupId );
			}
			else {
				return false;
			}
		}

		public int hashCode() {
			return userId.hashCode() + groupId.hashCode();
		}
	}

	public MembershipWithCompositeId() {
		super( new Id() );
	}

	public MembershipWithCompositeId(String name) {
		super( new Id(), name );
	}

	public void setGroup(Group group) {
		( (Id) getId() ).setGroupId( ( group == null ? null : group.getId() ) );
		super.setGroup( group );
	}

	public void setUser(User user) {
		( (Id) getId() ).setUserId( user == null ? null : user.getId() );
		super.setUser( user );
	}
}
