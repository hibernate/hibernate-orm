/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.join;
import java.util.Collection;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

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
