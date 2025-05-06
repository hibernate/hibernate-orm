/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 * Implementation of Address.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@link org.hibernate.testing.orm.domain.animal.Address} instead
 */
@Deprecated
public class Address {
	private String street;
	private String city;
	private String postalCode;
	private String country;
	private StateProvince stateProvince;

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

	public StateProvince getStateProvince() {
		return stateProvince;
	}

	public void setStateProvince(StateProvince stateProvince) {
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
		result = 31 * result + ( postalCode != null ? postalCode.hashCode() : 0 );
		result = 31 * result + ( country != null ? country.hashCode() : 0 );
		result = 31 * result + ( stateProvince != null ? stateProvince.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "Address{" +
				"street='" + street + '\'' +
				", city='" + city + '\'' +
				", postalCode='" + postalCode + '\'' +
				", country='" + country + '\'' +
				", stateProvince=" + stateProvince.getId() +
				'}';
	}
}
