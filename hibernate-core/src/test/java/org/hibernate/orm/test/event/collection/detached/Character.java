/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.detached;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
// "Character" is reserved in MySQL
@Table( name = "CharacterTable" )
public class Character implements Identifiable {
	private Integer id;
	private String name;
	private List<Alias> aliases = new ArrayList<Alias>();

	public Character() {
	}

	public Character(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany( cascade= CascadeType.ALL, mappedBy="characters" )
	public List<Alias> getAliases() {
		return aliases;
	}

	public void setAliases(List<Alias> aliases) {
		this.aliases = aliases;
	}

	public void associateAlias(Alias alias) {
		alias.getCharacters().add( this );
		getAliases().add( alias );
	}
}
