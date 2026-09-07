/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc;


/**
 * Boat implementation
 *
 * @author Steve Ebersole
 */
public class Boat {
	private Long id;
	private String tag;
	private Person driver;
	private Person boarder;

	public Boat() {
	}

	public Boat(String tag, Person driver, Person boarder) {
		this.tag = tag;
		this.driver = driver;
		this.boarder = boarder;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Person getDriver() {
		return driver;
	}

	public void setDriver(Person driver) {
		this.driver = driver;
	}

	public Person getBoarder() {
		return boarder;
	}

	public void setBoarder(Person boarder) {
		this.boarder = boarder;
	}
}
