package org.hibernate.test.bytecode.enhancement.join;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class Vehicle implements Serializable {

	@Id
	@GeneratedValue
	private Integer id;

	private String name;

	@OneToOne(optional = true, fetch = FetchType.LAZY)
	private Person driver;

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Person getDriver() {
		return driver;
	}

	public void setDriver(Person driver) {
		this.driver = driver;
	}

	public Vehicle() {
	}

	public Vehicle(String name) {
		this.name = name;
	}

}
