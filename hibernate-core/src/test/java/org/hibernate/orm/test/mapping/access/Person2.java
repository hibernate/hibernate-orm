/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "persons2")
public class Person2 {
	@Id
	public Integer id;

	@Embedded
	public Name2 name;

	@ElementCollection
	@CollectionTable( name = "person2_aliases" )
	@Embedded
	public Set<Name2> aliases;

	private Person2() {
		// for Hibernate use
	}

	public Person2(Integer id, Name2 name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public Name2 getName() {
		return name;
	}

	public void setName(Name2 name) {
		this.name = name;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Name2> getAliases() {
		return aliases;
	}

	public void setAliases(Set<Name2> aliases) {
		this.aliases = aliases;
	}

	public void addAlias(Name2 alias) {
		if ( aliases == null ) {
			aliases = new HashSet<>();
		}
		aliases.add( alias );
	}
}
