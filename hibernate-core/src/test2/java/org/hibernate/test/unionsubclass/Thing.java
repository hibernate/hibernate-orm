/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Thing.java 4418 2004-08-22 13:38:16Z oneovthafew $
package org.hibernate.test.unionsubclass;


/**
 * @author Gavin King
 */
public class Thing {
	private long id;
	private String description;
	private Being owner;
	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the owner.
	 */
	public Being getOwner() {
		return owner;
	}
	/**
	 * @param owner The owner to set.
	 */
	public void setOwner(Being owner) {
		this.owner = owner;
	}
}
