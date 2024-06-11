/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.collections.list;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;

/**
 * @author Steve Ebersole
 */
@Entity
public class Root {
	@Id
	private Integer id;
	@Basic
	private String name;
	@ElementCollection
	private List<String> tags;
	@ElementCollection
	@CollectionTable(name="root_categories")
	@OrderColumn(name = "position")
	private List<String> categories;
	@ManyToMany
	@CollectionTable(name="root_admins")
	private List<User> admins;


	protected Root() {
		// for Hibernate use
	}

	public Root(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
