/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class ProgramExecution {
	@Id
	@GeneratedValue
	private Integer id;
	private String action;
	@ManyToOne(fetch = FetchType.LAZY)
	private File appliesOn;


	public File getAppliesOn() {
		return appliesOn;
	}

	public void setAppliesOn(File appliesOn) {
		this.appliesOn = appliesOn;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
