/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.dereferenced;

import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

/**
 * @author Gail Badner
 */
@Entity
public class VersionedNoCascadeOne {
	@Id
	@GeneratedValue
	private long id;

	@OneToMany
	@JoinColumn
	private Set<Many> manies;

	@Version
	private long version;

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

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}
}
