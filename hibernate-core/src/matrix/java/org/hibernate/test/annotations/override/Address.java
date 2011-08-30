package org.hibernate.test.annotations.override;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Address {
	public String street;
	public String city; 
	public String state;

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getStreet() {

		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}
}
