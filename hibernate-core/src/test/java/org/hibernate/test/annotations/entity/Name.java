package org.hibernate.test.annotations.entity;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class Name extends FirstName {

	@Id
	@GeneratedValue
	private Integer id;

	@Embedded
	private LastName lastName;
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public LastName getLastName() {
		return lastName;
	}

	public void setLastName(LastName val) {
		this.lastName = val;
	}

}
