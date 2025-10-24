/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join;

import java.util.Collection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity( name = "sys_user" )
@Table( name = "`SYS_USER`" )
public class SysUserOrm {

	private long userid;

	private Collection<SysGroupsOrm> groups;

	@Id
	@GeneratedValue
	@Column( name = "`auid`" )
	public long getUserid() {
		return userid;
	}

	public void setUserid( long userid ) {
		this.userid = userid;
	}

	@ManyToMany( fetch = FetchType.LAZY )
	@JoinTable( name = "SYS_GROUPS_USERS",
			joinColumns = @JoinColumn( name = "USERID", referencedColumnName = "`auid`" ),
			inverseJoinColumns = @JoinColumn( name = "GROUPID", referencedColumnName = "GROUPID" ) )
	public Collection<SysGroupsOrm> getGroups() {
		return groups;
	}

	public void setGroups( Collection<SysGroupsOrm> groups ) {
		this.groups = groups;
	}
}
