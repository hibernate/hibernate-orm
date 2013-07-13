//$Id$
package org.hibernate.test.annotations.onetomany;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Model {
	@Id
	@GeneratedValue
	private Integer id;

	@ManyToOne
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
