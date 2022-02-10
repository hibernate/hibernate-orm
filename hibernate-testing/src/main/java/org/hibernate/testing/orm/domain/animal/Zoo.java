/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.domain.animal;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

@Entity
@Inheritance
@DiscriminatorColumn( name = "zooType" )
@DiscriminatorValue( "Z" )
public class Zoo {
	private Long id;
	private String name;
	private Classification classification;
	private Map directors = new HashMap();
	private Map animals = new HashMap();
	private Map mammals = new HashMap();
	private Address address;

	public Zoo() {
	}

	public Zoo(String name, Address address) {
		this.name = name;
		this.address = address;
	}

	@Id
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

	@ManyToMany
	@JoinTable(
			name = "t_directors",
			joinColumns = @JoinColumn( name = "zoo_fk" ),
			inverseJoinColumns = @JoinColumn( name = "director_fk" )
	)
	@MapKeyColumn( name = "`title`" )
	public Map<String,Human> getDirectors() {
		return directors;
	}

	public void setDirectors(Map directors) {
		this.directors = directors;
	}

	@OneToMany
	@JoinColumn( name = "mammal_fk" )
	@MapKeyColumn( name = "name" )
	public Map<String,Mammal> getMammals() {
		return mammals;
	}

	public void setMammals(Map mammals) {
		this.mammals = mammals;
	}

	@OneToMany( mappedBy = "zoo" )
	@MapKeyColumn( name = "serialNumber" )
	public Map<String, Animal> getAnimals() {
		return animals;
	}

	public void setAnimals(Map animals) {
		this.animals = animals;
	}

	@Embedded
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@Enumerated( value = EnumType.STRING )
	public Classification getClassification() {
		return classification;
	}

	public void setClassification(Classification classification) {
		this.classification = classification;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof Zoo ) ) {
			return false;
		}

		Zoo zoo = ( Zoo ) o;

		if ( address != null ? !address.equals( zoo.address ) : zoo.address != null ) {
			return false;
		}
		if ( name != null ? !name.equals( zoo.name ) : zoo.name != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( address != null ? address.hashCode() : 0 );
		return result;
	}
}
