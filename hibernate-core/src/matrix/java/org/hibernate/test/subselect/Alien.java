//$Id: Alien.java 7203 2005-06-19 02:01:05Z oneovthafew $
package org.hibernate.test.subselect;


/**
 * @author Gavin King
 */
public class Alien {
	private Long id;
	private String identity;
	private String planet;
	private String species;
	private double heightInches;
	
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	public String getIdentity() {
		return identity;
	}
	public void setSpecies(String species) {
		this.species = species;
	}
	public String getSpecies() {
		return species;
	}
	public void setPlanet(String planet) {
		this.planet = planet;
	}
	public String getPlanet() {
		return planet;
	}
	public double getHeightInches() {
		return heightInches;
	}
	public void setHeightInches(double heightInches) {
		this.heightInches = heightInches;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}
}
