/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Top.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.legacy;


public class Top {
	private long id;
	private String name;
	private String address;
	private int count;
	private java.util.Date date;
	private Top other;
	private Top top;
	
	public Top(int c) {
		count=c;
	}

	public Top() {
	}
	
	public void init() {
		name="Someone With Along Name";
		address="1234 Some Street, Some City, Victoria, 3000, Austraya";
		count=69;
		date=new java.sql.Date(666);
	}
	/**
	 * Gets the name
	 * @return Returns a String
	 */
	public String getName() {
		return name;
	}
	/**
	 * Sets the name
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the address
	 * @return Returns a String
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * Sets the address
	 * @param address The address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	
	/**
	 * Gets the count
	 * @return Returns a int
	 */
	public int getCount() {
		return count;
	}
	/**
	 * Sets the count
	 * @param count The count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}
	
	/**
	 * Gets the date
	 * @return Returns a java.util.Date
	 */
	public java.util.Date getDate() {
		return date;
	}
	/**
	 * Sets the date
	 * @param date The date to set
	 */
	public void setDate(java.util.Date date) {
		this.date = date;
	}
	
	/**
	 * Returns the other.
	 * @return Simple
	 */
	public Top getOther() {
		return other;
	}
	
	/**
	 * Sets the other.
	 * @param other The other to set
	 */
	public void setOther(Top other) {
		this.other = other;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long l) {
		id = l;
	}

}







