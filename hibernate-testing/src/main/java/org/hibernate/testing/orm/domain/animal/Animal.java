/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.domain.animal;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

@Entity
@Inheritance( strategy = InheritanceType.JOINED )
public class Animal {
	private Long id;
	private float bodyWeight;
	private Set offspring;
	private Animal mother;
	private Animal father;
	private String description;
	private Zoo zoo;
	private String serialNumber;

	public Animal() {
	}

	public Animal(String description, float bodyWeight) {
		this.description = description;
		this.bodyWeight = bodyWeight;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column( name = "body_weight" )
	public float getBodyWeight() {
		return bodyWeight;
	}

	public void setBodyWeight(float bodyWeight) {
		this.bodyWeight = bodyWeight;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	@ManyToOne
	@JoinColumn( name = "zoo_fk" )
	public Zoo getZoo() {
		return zoo;
	}

	public void setZoo(Zoo zoo) {
		this.zoo = zoo;
	}

	@ManyToOne
	@JoinColumn( name = "mother_fk" )
	public Animal getMother() {
		return mother;
	}

	public void setMother(Animal mother) {
		this.mother = mother;
	}

	@ManyToOne
	@JoinColumn( name = "father_fk" )
	public Animal getFather() {
		return father;
	}

	public void setFather(Animal father) {
		this.father = father;
	}

	@OneToMany
	@JoinColumn( name = "mother_fk" )
	@OrderBy( "father_fk" )
	public Set<Human> getOffspring() {
		return offspring;
	}

	public void addOffspring(Animal offspring) {
		if ( this.offspring == null ) {
			this.offspring = new HashSet();
		}

		this.offspring.add( offspring );
	}

	public void setOffspring(Set offspring) {
		this.offspring = offspring;
	}
}
