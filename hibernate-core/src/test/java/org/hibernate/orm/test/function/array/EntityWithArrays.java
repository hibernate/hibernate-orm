/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.array;

import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
@Entity
public class EntityWithArrays {

	@Id
	private Long id;

	@Column(name = "the_array")
	private String[] theArray;

	@Column(name = "the_array", insertable = false, updatable = false)
	private List<String> theCollection;

	@Column(name = "the_label")
	private Label theLabel;

	@Column(name = "the_labels")
	private Set<Label> theLabels;

	public EntityWithArrays() {
	}

	public EntityWithArrays(Long id, String[] theArray) {
		this.id = id;
		this.theArray = theArray;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String[] getTheArray() {
		return theArray;
	}

	public void setTheArray(String[] theArray) {
		this.theArray = theArray;
	}

	public List<String> getTheCollection() {
		return theCollection;
	}

	public void setTheCollection(List<String> theCollection) {
		this.theCollection = theCollection;
	}

	public Set<Label> getTheLabels() {
		return theLabels;
	}

	public void setTheLabels(Set<Label> theLabels) {
		this.theLabels = theLabels;
	}
}
