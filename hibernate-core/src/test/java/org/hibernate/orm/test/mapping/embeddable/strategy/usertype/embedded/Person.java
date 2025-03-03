/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CompositeType;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Table(name = "people")
//tag::embeddable-usertype-property[]
@Entity
public class Person {
	@Id
	public Integer id;

	@Embedded
	@AttributeOverride(name = "firstName", column = @Column(name = "first_name"))
	@AttributeOverride(name = "lastName", column = @Column(name = "last_name"))
	@CompositeType( NameCompositeUserType.class )
	public Name name;

	@ElementCollection
	@AttributeOverride(name = "firstName", column = @Column(name = "first_name"))
	@AttributeOverride(name = "lastName", column = @Column(name = "last_name"))
	@CompositeType( NameCompositeUserType.class )
	public Set<Name> aliases;

	//end::embeddable-usertype-property[]

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

//tag::embeddable-usertype-property[]
}
//end::embeddable-usertype-property[]
