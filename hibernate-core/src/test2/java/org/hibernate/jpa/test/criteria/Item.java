/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Chris Cranford
 */
@Entity
public class Item {
	@Id
	@GeneratedValue
	private Integer id;

	private String name;

	@ElementCollection
	@CollectionTable(name = "item_roles")
	@Convert(converter = ItemAttributeConverter.class)
	public List<Attribute> roles = new ArrayList<>();

	Item() {

	}

	Item(String name) {
		this.name = name;
	}

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

	public List<Attribute> getRoles() {
		return roles;
	}

	public void setRoles(List<Attribute> roles) {
		this.roles = roles;
	}
}
