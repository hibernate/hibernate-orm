/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Id other = (Id) obj;
			if (userId == null) {
				if (other.userId != null)
					return false;
			} else if (!userId.equals(other.userId))
				return false;
			if (groupId == null) {
				if (other.groupId != null)
					return false;
			} else if (!groupId.equals(other.groupId))
				return false;
			return true;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((userId == null) ? 0 : userId.hashCode());
			result = prime * result
					+ ((groupId == null) ? 0 : groupId.hashCode());
			return result;
		}
	}

	public MembershipWithCompositeId() {
		super( new Id() );
	}

	public MembershipWithCompositeId(String name) {
		super( new Id(), name );
	}

	public void setGroup(Group group) {
		if (getId() == null) {
			setId(new Id());
		}
		( (Id) getId() ).setGroupId( ( group == null ? null : group.getId() ) );
		super.setGroup( group );
	}

	public void setUser(User user) {
		if (getId() == null) {
			setId(new Id());
		}
		( (Id) getId() ).setUserId( user == null ? null : user.getId() );
		super.setUser( user );
	}
}
