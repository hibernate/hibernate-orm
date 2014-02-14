//$Id$
package org.hibernate.test.annotations.access;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Access(AccessType.FIELD)
public class Furniture extends Woody {
	@Id
	@GeneratedValue
	private Integer id;

	private String brand;

	@Transient
	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public long weight;

	@Access(AccessType.PROPERTY)
	public long getWeight() {
		return weight + 1;
	}

	public void setWeight(long weight) {
		this.weight = weight + 1;
	}
}
