/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Item.java 8670 2005-11-25 17:36:29Z epbernard $

package org.hibernate.test.mixed;



/**
 * @author Gavin King
 */

public abstract class Item {

	private Long id;

	private String name;

	private Folder parent;

	/**
	 * @return Returns the parent.
	 */

	public Folder getParent() {

		return parent;

	}

	/**
	 * @param parent The parent to set.
	 */

	public void setParent(Folder parent) {

		this.parent = parent;

	}

	/**
	 * @return Returns the id.
	 */

	public Long getId() {

		return id;

	}

	/**
	 * @param id The id to set.
	 */

	public void setId(Long id) {

		this.id = id;

	}

	/**
	 * @return Returns the name.
	 */

	public String getName() {

		return name;

	}

	/**
	 * @param name The name to set.
	 */

	public void setName(String name) {

		this.name = name;

	}

}

