/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embedded.one2many;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;

import jakarta.persistence.Access;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Embeddable
@Access(AccessType.PROPERTY)
public class PersonName extends Name {
	private Set<Alias> aliases = new HashSet<Alias>();

	public PersonName() {
	}

	public PersonName(String first, String last) {
		super( first, last );
	}

	@OneToMany( cascade = CascadeType.ALL )
	public Set<Alias> getAliases() {
		return aliases;
	}

	public void setAliases(Set<Alias> aliases) {
		this.aliases = aliases;
	}
}
