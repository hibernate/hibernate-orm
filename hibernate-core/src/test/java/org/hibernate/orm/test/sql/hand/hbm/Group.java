/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.hand.hbm;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table( name = "groups" )
public class Group {
	@Id
	private Integer id;
	private String name;

	@ManyToOne
	@JoinColumn( name = "admin_fk" )
	private Employee admin;

	@ManyToMany
	@JoinTable(
			name = "members",
			joinColumns = @JoinColumn( name = "group_fk" ),
			inverseJoinColumns = @JoinColumn( name = "employee_fk" )
	)
	private Set<Employee> members;

	private Group() {
		// for Hibernate use
	}

	public Group(Integer id, String name, Employee admin) {
		this.id = id;
		this.name = name;
		this.admin = admin;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Employee getAdmin() {
		return admin;
	}

	public void setAdmin(Employee admin) {
		this.admin = admin;
	}

	public Set<Employee> getMembers() {
		return members;
	}

	public void setMembers(Set<Employee> members) {
		this.members = members;
	}

	public void addMember(Employee member) {
		if ( members == null ) {
			members = new HashSet<>();
		}
		members.add( member );
	}
}
