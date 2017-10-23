/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.ordered.joinedInheritence;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Zoo {
	private Long id;
	private String name;
	private String city;
	private Set<Tiger> tigers = new HashSet<Tiger>();
	private Set<Lion> lions = new HashSet<Lion>();
	private Set<Animal> animals = new HashSet<Animal>();

	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( strategy = "increment", name = "increment" )
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

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@OneToMany
	@JoinColumn
	@javax.persistence.OrderBy( "weight" )
	public Set<Tiger> getTigers() {
		return tigers;
	}

	public void setTigers(Set<Tiger> tigers) {
		this.tigers = tigers;
	}

	@OneToMany
	@JoinColumn
	@org.hibernate.annotations.OrderBy( clause = "weight" )
	public Set<Lion> getLions() {
		return lions;
	}

	public void setLions(Set<Lion> lions) {
		this.lions = lions;
	}

	@OneToMany
	@JoinColumn
	@javax.persistence.OrderBy( "id asc" ) // HHH-7630 ensure explicitly naming the superclass id works
	public Set<Animal> getAnimalsById() {
		return animals;
	}

	public void setAnimalsById(Set<Animal> animals) {
		this.animals = animals;
	}
}
