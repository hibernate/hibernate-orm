/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table( name = "SYS_GROUPS" )
public class SysGroupsOrm {

	private long groupId;

	@Id
	@GeneratedValue
	@Column( name = "GROUPID" )
	public long getGroupId() {
		return groupId;
	}

	public void setGroupId( long groupId ) {
		this.groupId = groupId;
	}

}
