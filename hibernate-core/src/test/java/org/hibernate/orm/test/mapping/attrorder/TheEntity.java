/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attrorder;

import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.NaturalId;

/**
 * @author Steve Ebersole
 */
@Entity
public class TheEntity {
	@Id
	private Integer id;

	@NaturalId
	private String userCode;
	@NaturalId
	private String assignment;

	private String name;

	@Embedded
	private TheComponent theComponent;

	@ElementCollection
	private Set<TheComponent> theComponents;

	public TheEntity() {
	}

	public TheEntity(Integer id, String userCode, String assignment, String name, TheComponent theComponent, Set<TheComponent> theComponents) {
		this.id = id;
		this.userCode = userCode;
		this.assignment = assignment;
		this.name = name;
		this.theComponent = theComponent;
		this.theComponents = theComponents;
	}

	public Integer getId() {
		return id;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getAssignment() {
		return assignment;
	}

	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TheComponent getTheComponent() {
		return theComponent;
	}

	public void setTheComponent(TheComponent theComponent) {
		this.theComponent = theComponent;
	}

	public Set<TheComponent> getTheComponents() {
		return theComponents;
	}

	public void setTheComponents(Set<TheComponent> theComponents) {
		this.theComponents = theComponents;
	}
}
