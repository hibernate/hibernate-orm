package org.hibernate.orm.test.boot.models.xml.dynamic;

import java.util.List;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Employee {
	@Id
	private String name;
	@Id
	private int number;

	@OneToMany
	private List<Address> addresses;
}
