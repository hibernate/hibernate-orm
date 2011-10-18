/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2004-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.legacy;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Simple implements Serializable {
	private Long id;
	private String name;
	private String address;
	private int count;
	private java.util.Date date;
	private Float number;
	private Simple other;

	private Long parent;

	public Simple(Long id) {
		this.id = id;
	}

	public Simple(int c) {
		count=c;
	}
	public Simple() {}

	public void init() {
		name="Someone With Along Name";
		address="1234 Some Street, Some City, Victoria, 3000, Austraya";
		count=69;
		date=new java.sql.Date(666);
		number=new Float(55.8);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
	 * Gets the pay number
	 * @return Returns a Float
	 */
	public Float getPay() {
		return number;
	}

	/**
	 * Sets the pay number
	 * @param number The Pay to set
	 */
	public void setPay(Float number) {
		this.number = number;
	}

	/**
	 * Returns the other.
	 * @return Simple
	 */
	public Simple getOther() {
		return other;
	}

	/**
	 * Sets the other.
	 * @param other The other to set
	 */
	public void setOther(Simple other) {
		this.other = other;
	}

}







