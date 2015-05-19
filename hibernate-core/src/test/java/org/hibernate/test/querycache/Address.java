/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: Address.java 7996 2005-08-22 14:49:57Z steveebersole $
package org.hibernate.test.querycache;


/**
 * Implementation of Address.
 *
 * @author Steve Ebersole
 */
public class Address {
	private long id;
	private String addressType;
	private String street;
	private String city;
	private String stateProvince;
	private String postalCode;
	private String country;
	private Student student;

	public Address() {}

	public Address(Student student, String type, String street, String city, String stateProvince, String postalCode, String country) {
		this.student = student;
		this.addressType = type;
		this.street = street;
		this.city = city;
		this.stateProvince = stateProvince;
		this.postalCode = postalCode;
		this.country = country;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public String getAddressType() {
		return addressType;
	}

	public void setAddressType(String addressType) {
		this.addressType = addressType;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getStateProvince() {
		return stateProvince;
	}

	public void setStateProvince(String stateProvince) {
		this.stateProvince = stateProvince;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Address address = ( Address ) o;

		if ( city != null ? !city.equals( address.city ) : address.city != null ) {
			return false;
		}
		if ( country != null ? !country.equals( address.country ) : address.country != null ) {
			return false;
		}
		if ( postalCode != null ? !postalCode.equals( address.postalCode ) : address.postalCode != null ) {
			return false;
		}
		if ( stateProvince != null ? !stateProvince.equals( address.stateProvince ) : address.stateProvince != null ) {
			return false;
		}
		if ( street != null ? !street.equals( address.street ) : address.street != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = street != null ? street.hashCode() : 0;
		result = 31 * result + ( city != null ? city.hashCode() : 0 );
		result = 31 * result + ( stateProvince != null ? stateProvince.hashCode() : 0 );
		result = 31 * result + ( postalCode != null ? postalCode.hashCode() : 0 );
		result = 31 * result + ( country != null ? country.hashCode() : 0 );
		return result;
	}
}
