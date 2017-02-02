package org.hibernate.test.bytecode.enhancement.join;

import java.io.Serializable;
import javax.persistence.*;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
public class Person implements Serializable {
	@Id
	@GeneratedValue
	private Integer id;

	private String name;

	@OneToOne(optional = true, mappedBy = "driver", fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private Vehicle vehicle;

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
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

	public Person() {
	}

	public Person(String name) {
		this.name = name;
	}
}