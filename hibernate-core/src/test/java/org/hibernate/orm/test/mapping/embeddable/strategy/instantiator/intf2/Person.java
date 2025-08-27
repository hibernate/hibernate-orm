/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf2;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.EmbeddableInstantiator;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "people")
public class Person {
	@Id
	public Integer id;

	@Embedded
	@EmbeddableInstantiator( NameInstantiator.class )
	@Access( AccessType.PROPERTY )
	public Name name;

	@ElementCollection
	@Embedded
	@EmbeddableInstantiator( NameInstantiator.class )
	@Access( AccessType.PROPERTY )
	public Set<Name> aliases;

	private Person() {
		// for Hibernate use
	}

	public Person(Integer id, Name name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Name> getAliases() {
		return aliases;
	}

	public void setAliases(Set<Name> aliases) {
		this.aliases = aliases;
	}

	public void addAlias(Name alias) {
		if ( aliases == null ) {
			aliases = new HashSet<>();
		}
		aliases.add( alias );
	}

}
