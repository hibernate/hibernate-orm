/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.id.sequences.entities;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;


/**
 * Test entity for enum type as id.
 * 
 * @author Hardy Ferentschik
 * @see ANN-744
 */
@Entity
public class PlanetCheatSheet {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "planet")
	private Planet planet;

	private double mass;

	private double radius;

	private long numberOfInhabitants;

	public Planet getPlanet() {
		return planet;
	}

	public void setPlanet(Planet planet) {
		this.planet = planet;
	}

	public double getMass() {
		return mass;
	}

	public void setMass(double mass) {
		this.mass = mass;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public long getNumberOfInhabitants() {
		return numberOfInhabitants;
	}

	public void setNumberOfInhabitants(long numberOfInhabitants) {
		this.numberOfInhabitants = numberOfInhabitants;
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	    
	    String retValue = "";
	    
	    retValue = "PlanetCheatSheet ( "
	        + super.toString() + TAB
	        + "planet = " + this.planet + TAB
	        + "mass = " + this.mass + TAB
	        + "radius = " + this.radius + TAB
	        + "numberOfInhabitants = " + this.numberOfInhabitants + TAB
	        + " )";
	
	    return retValue;
	}	
}
