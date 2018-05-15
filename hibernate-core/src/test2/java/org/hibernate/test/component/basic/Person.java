/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Person.java 11345 2007-03-26 17:24:20Z steve.ebersole@jboss.com $
package org.hibernate.test.component.basic;
import java.util.Date;

/**
 * @author Gavin King
 */
public class Person {
	private String name;
	private Date dob;
	private String address;
	private String currentAddress;
	private String previousAddress;
	private int yob;
	private double heightInches;
	Person() {}
	public Person(String name, Date dob, String address) {
		this.name = name;
		this.dob = dob;
		this.address = address;
		this.currentAddress = address;
	}
	public int getYob() {
		return yob;
	}
	public void setYob(int age) {
		this.yob = age;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public Date getDob() {
		return dob;
	}
	public void setDob(Date dob) {
		this.dob = dob;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPreviousAddress() {
		return previousAddress;
	}
	public void setPreviousAddress(String previousAddress) {
		this.previousAddress = previousAddress;
	}
	public void changeAddress(String add) {
		setPreviousAddress( getAddress() );
		setAddress(add);
	}
	public String getCurrentAddress() {
		return currentAddress;
	}
	public void setCurrentAddress(String currentAddress) {
		this.currentAddress = currentAddress;
	}
	public double getHeightInches() {
		return heightInches;
	}
	public void setHeightInches(double heightInches) {
		this.heightInches = heightInches;
	}	
}
