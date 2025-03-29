/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Table(name = "STUDENT")
@SequenceGenerator(name = "SEQ_STUDENT", sequenceName = "SEQ_STUDENT", allocationSize = 1)
@Audited
public class Student {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STUDENT")
	private Long id;

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "students")
	private Set<Professor> professors = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<Professor> getProfessors() {
		return professors;
	}

	public void setProfessors(Set<Professor> professors) {
		this.professors = professors;
	}
}
