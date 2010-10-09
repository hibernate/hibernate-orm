package org.hibernate.test.annotations.join;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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