/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.dereferenced;

import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

/**
 * @author Gail Badner
 */
@Entity
public class UnversionedCascadeOne {
	@Id
	@GeneratedValue
	private long id;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn
	private Set<Many> manies;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public Set<Many> getManies() {
		return manies;
	}
	public void setManies(Set<Many> manies) {
		this.manies = manies;
	}
}
