/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection.detached;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

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
