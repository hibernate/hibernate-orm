package org.hibernate.jpa.test.criteria;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Animal
 *
 */
@Entity
@Table( name = "ANIMAL" )
public class Animal {
	private Long id;
	private Animal mother;
	private Animal father;
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	public Animal getMother() {
		return mother;
	}

	public void setMother(Animal mother) {
		this.mother = mother;
	}

	@ManyToOne
	public Animal getFather() {
		return father;
	}

	public void setFather(Animal father) {
		this.father = father;
	}
}
