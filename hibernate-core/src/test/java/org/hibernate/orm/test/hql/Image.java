/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 * @author Rob.Hasselbaum
 */
public class Image {

	private Long id;
	private String name;
	private double sizeKb;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the size in kb
	 */
	public double getSizeKb() {
		return sizeKb;
	}
	/**
	 * @param sizeKb the size in kb to set
	 */
	public void setSizeKb(double sizeKb) {
		this.sizeKb = sizeKb;
	}

}
