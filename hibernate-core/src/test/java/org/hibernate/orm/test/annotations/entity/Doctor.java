/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.SQLRestriction;

/**
 *
 * @author Vlad Smith
 *
 */
@Entity
@SQLRestriction("yearsExperience > 3")
public class Doctor {
	private Integer id;
	private String name;
	private boolean activeLicense = false;
	private Integer yearsExperience = 0;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public boolean isActiveLicense() {
		return activeLicense;
	}

	public void setActiveLicense(boolean activeLicense) {
		this.activeLicense = activeLicense;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getYearsExperience() {
		return yearsExperience;
	}

	public void setYearsExperience(Integer yearsExperience) {
		this.yearsExperience = yearsExperience;
	}



}
