/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Location.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.io.Serializable;
import java.util.Locale;

public class Location implements Serializable {
	private int streetNumber;
	private String city;
	private String streetName;
	private String countryCode;
	private Locale locale;
	private String description;
	
	/**
	 * Returns the countryCode.
	 * @return String
	 */
	public String getCountryCode() {
		return countryCode;
	}
	
	/**
	 * Returns the description.
	 * @return String
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns the locale.
	 * @return Locale
	 */
	public Locale getLocale() {
		return locale;
	}
	
	/**
	 * Returns the streetName.
	 * @return String
	 */
	public String getStreetName() {
		return streetName;
	}
	
	/**
	 * Returns the streetNumber.
	 * @return int
	 */
	public int getStreetNumber() {
		return streetNumber;
	}
	
	/**
	 * Sets the countryCode.
	 * @param countryCode The countryCode to set
	 */
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	
	/**
	 * Sets the description.
	 * @param description The description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Sets the locale.
	 * @param locale The locale to set
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	
	/**
	 * Sets the streetName.
	 * @param streetName The streetName to set
	 */
	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}
	
	/**
	 * Sets the streetNumber.
	 * @param streetNumber The streetNumber to set
	 */
	public void setStreetNumber(int streetNumber) {
		this.streetNumber = streetNumber;
	}
	
	/**
	 * Returns the city.
	 * @return String
	 */
	public String getCity() {
		return city;
	}
	
	/**
	 * Sets the city.
	 * @param city The city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}
	
	public boolean equals(Object other) {
		Location l = (Location) other;
		return l.getCity().equals(city) && l.getStreetName().equals(streetName) && l.getCountryCode().equals(countryCode) && l.getStreetNumber()==streetNumber;
	}
	public int hashCode() {
		return streetName.hashCode();
	}
	
}






