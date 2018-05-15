/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e1.a;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cascade;


/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(DependentId.class)
public class Dependent {
	private String name;
	// id attribute mapped by join column default
	private Employee emp;

	public Dependent() {
	}

	public Dependent(String name, Employee emp) {
		this.name = name;
		this.emp = emp;
	}

	@Id
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
	@ManyToOne( cascade = CascadeType.PERSIST )
	@Cascade( org.hibernate.annotations.CascadeType.SAVE_UPDATE )
	@JoinColumn(nullable=false)
	public Employee getEmp() {
		return emp;
	}

	public void setEmp(Employee emp) {
		this.emp = emp;
	}
}
