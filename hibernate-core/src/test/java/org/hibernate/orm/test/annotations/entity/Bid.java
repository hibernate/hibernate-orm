/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Bid {
	private Integer id;
	private String description;
	private Starred note;
	private Starred editorsNote;
	private Boolean approved;

	@Enumerated(EnumType.STRING)
	//@Column(columnDefinition = "VARCHAR(10)")
	public Starred getEditorsNote() {
		return editorsNote;
	}

	public void setEditorsNote(Starred editorsNote) {
		this.editorsNote = editorsNote;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Starred getNote() {
		return note;
	}

	public void setNote(Starred note) {
		this.note = note;
	}

	public Boolean getApproved() {
		return approved;
	}

	public void setApproved(Boolean approved) {
		this.approved = approved;
	}

}
