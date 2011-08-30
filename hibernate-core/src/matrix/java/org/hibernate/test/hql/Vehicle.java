// $Id: Vehicle.java 7087 2005-06-08 18:23:44Z steveebersole $
package org.hibernate.test.hql;


/**
 * Implementation of Vehicle.
 *
 * @author Steve Ebersole
 */
public abstract class Vehicle {
	private Long id;
	private String vin;
	private String owner;

	public String getVin() {
		return vin;
	}

	public void setVin(String vin) {
		this.vin = vin;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
}
