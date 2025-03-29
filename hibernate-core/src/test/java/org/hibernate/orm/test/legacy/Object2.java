/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 *
 * @author Wolfgang Voelkl
 *
 */
public class Object2 {
	private Long id;
	private String dummy;
	private MainObject belongsToMainObj;

	public Long getId() {
		return id;
	}

	public void setId(Long l) {
		this.id = l;
	}

	public String getDummy() {
		return dummy;
	}

	public void setDummy(String string) {
		dummy = string;
	}

	public MainObject getBelongsToMainObj() {
		return belongsToMainObj;
	}

	public void setBelongsToMainObj(MainObject object) {
		belongsToMainObj = object;
	}

}
