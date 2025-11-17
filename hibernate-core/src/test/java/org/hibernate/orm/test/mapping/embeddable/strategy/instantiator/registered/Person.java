/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.registered; /**
 * @author Steve Ebersole
 */

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.EmbeddableInstantiatorRegistration;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Table(name = "persons")
//tag::embeddable-instantiator-registration[]
@Entity
@EmbeddableInstantiatorRegistration( embeddableClass = Name.class, instantiator = NameInstantiator.class )
public class Person {
	@Id
	public Integer id;
	@Embedded
	public Name name;
	@ElementCollection
	@Embedded
	public Set<Name> aliases;

	//end::embeddable-instantiator-registration[]

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

//tag::embeddable-instantiator-registration[]
}
//end::embeddable-instantiator-registration[]
