/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deletetransient;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Gail Badner
 */
@Entity
@Table( name="suites")
public class Suite {
	@Id
	private Integer id;
	private String location;
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name="suite_fk")
	private Set<Note> notes = new HashSet<>();

	public Suite() {
	}

	public Suite(Integer id, String location) {
		this.id = id;
		this.location = location;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Set getNotes() {
		return notes;
	}

	public void setNotes(Set notes) {
		this.notes = notes;
	}
}
