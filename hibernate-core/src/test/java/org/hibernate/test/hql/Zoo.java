//$Id: Zoo.java 10653 2006-10-26 13:38:50Z steve.ebersole@jboss.com $
package org.hibernate.test.hql;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gavin King
 */
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

	public Map getDirectors() {
		return directors;
	}

	public void setDirectors(Map directors) {
		this.directors = directors;
	}

	public Map getMammals() {
		return mammals;
	}

	public void setMammals(Map mammals) {
		this.mammals = mammals;
	}

	public Map getAnimals() {
		return animals;
	}

	public void setAnimals(Map animals) {
		this.animals = animals;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

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
