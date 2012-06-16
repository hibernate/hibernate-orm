//$Id: Human.java 7203 2005-06-19 02:01:05Z oneovthafew $
package org.hibernate.test.subselect;


/**
 * @author Gavin King
 */
public class Human {
	private Long id;
	private String name;
	private char sex;
	private String address;
	private double heightInches;
	
	public void setAddress(String address) {
		this.address = address;
	}
	public String getAddress() {
		return address;
	}
	public void setSex(char sex) {
		this.sex = sex;
	}
	public char getSex() {
		return sex;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}
	public double getHeightInches() {
		return heightInches;
	}
	public void setHeightInches(double heightInches) {
		this.heightInches = heightInches;
	}
}
