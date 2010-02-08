package org.hibernate.test.annotations.derivedidentities.e1.a;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
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
	public Employee getEmp() {
		return emp;
	}

	public void setEmp(Employee emp) {
		this.emp = emp;
	}
}
