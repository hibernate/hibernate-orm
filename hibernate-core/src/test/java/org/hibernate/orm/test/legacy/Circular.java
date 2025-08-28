/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

public class Circular {

	private String id;
	private Class clazz;
	private Circular other;
	private Object anyEntity;

	/**
	 * Constructor for Circular.
	 */
	public Circular() {
		super();
	}

	/**
	 * Returns the clazz.
	 * @return Class
	 */
	public Class getClazz() {
		return clazz;
	}

	/**
	 * Returns the id.
	 * @return String
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the clazz.
	 * @param clazz The clazz to set
	 */
	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the other.
	 * @return Circular
	 */
	public Circular getOther() {
		return other;
	}

	/**
	 * Sets the other.
	 * @param other The other to set
	 */
	public void setOther(Circular other) {
		this.other = other;
	}

	/**
	 * Returns the anyEntity.
	 * @return Object
	 */
	public Object getAnyEntity() {
		return anyEntity;
	}

	/**
	 * Sets the anyEntity.
	 * @param anyEntity The anyEntity to set
	 */
	public void setAnyEntity(Object anyEntity) {
		this.anyEntity = anyEntity;
	}

}
