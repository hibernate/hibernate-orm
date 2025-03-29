/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.norevision;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

@Audited
@Entity
public class Person implements Serializable {
	@Id
	@GeneratedValue
	private Integer id;
	@AuditMappedBy(mappedBy = "person")
	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "person_id")
	private Set<Name> names;

	public Person() {
		names = new HashSet<Name>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Name> getNames() {
		return names;
	}

	public void setNames(Set<Name> names) {
		this.names = names;
	}
}
