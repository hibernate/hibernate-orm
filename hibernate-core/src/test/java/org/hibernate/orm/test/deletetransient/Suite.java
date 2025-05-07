/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deletetransient;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Gail Badner
 */
public class Suite {
	private Long id;
	private String location;
	private Set<Note> notes = new HashSet<>();

	public Suite() {
	}

	public Suite(String location) {
		this.location = location;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Set<Note> getNotes() {
		return notes;
	}

	public void setNotes(Set<Note> notes) {
		this.notes = notes;
	}
}
