/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.toone;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class User {
	private Integer id;
	private PersonalInfo personalInfo;

	public User() {
	}

	public User(Integer id, PersonalInfo personalInfo) {
		this.id = id;
		this.personalInfo = personalInfo;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public PersonalInfo getPersonalInfo() {
		return personalInfo;
	}

	public void setPersonalInfo(PersonalInfo personalInfo) {
		this.personalInfo = personalInfo;
	}
}
