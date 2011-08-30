//$Id: Being.java 7203 2005-06-19 02:01:05Z oneovthafew $
package org.hibernate.test.subselect;


/**
 * @author Gavin King
 */
public class Being {
	private long id;
	private String identity;
	private String location;
	private String species;
	private double heightInches;	
	
	public void setLocation(String location) {
		this.location = location;
	}
	public String getLocation() {
		return location;
	}
	public void setSpecies(String species) {
		this.species = species;
	}
	public String getSpecies() {
		return species;
	}
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	public String getIdentity() {
		return identity;
	}
	public double getHeightInches() {
		return heightInches;
	}
	public void setHeightInches(double heightInches) {
		this.heightInches = heightInches;
	}
}
