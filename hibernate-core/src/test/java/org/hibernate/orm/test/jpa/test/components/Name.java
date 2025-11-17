/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.test.components;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;

/**
 * The name component
 *
 * @author alan.oleary
 */
@Embeddable
public class Name implements Serializable {
	private static final long serialVersionUID = 8381969086665589013L;

	private String firstName;
	private String lastName;
	private Set<Alias> aliases = new HashSet<Alias>(  );

	public Name() {
	}

	public Name(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Column(name = "FIRST_NAME", nullable = false)
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column(name = "LAST_NAME", nullable = false)
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@OneToMany(cascade = CascadeType.ALL)
	public Set<Alias> getAliases() {
		return aliases;
	}

	public void setAliases(Set<Alias> aliases) {
		this.aliases = aliases;
	}
}
