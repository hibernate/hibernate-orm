//$Id$
package org.hibernate.test.annotations.xml.ejb3;

/**
 * @author Emmanuel Bernard
 */
public class Model {
	private Integer id;
	private Manufacturer manufacturer;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Manufacturer getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(Manufacturer manufacturer) {
		this.manufacturer = manufacturer;
	}
}
