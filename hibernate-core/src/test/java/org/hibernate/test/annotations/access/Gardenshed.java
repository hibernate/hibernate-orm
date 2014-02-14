//$Id$
package org.hibernate.test.annotations.access;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.annotations.AttributeAccessor;

/**
 * This is the opposite of the Furniture test, as this tries to override the class AccessType("property") with
 * the property AccessType("field").
 *
 * @author Dennis Fleurbaaij
 * @since 2007-05-31
 */
@Entity
@AttributeAccessor( "property" )
public class Gardenshed
		extends
		Woody {
	private Integer id;
	private String brand;
	public long floors;

	@Transient
	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	// These 2 functions should not return in Hibernate, but the value should come from the field "floors"
	@AttributeAccessor( "field" )
	public long getFloors() {
		return this.floors + 2;
	}

	public void setFloors(long floors) {
		this.floors = floors + 2;
	}
}
