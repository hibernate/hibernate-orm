package org.hibernate.test.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Relationship {

	@Id
	private Long id;
	private String name;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}
