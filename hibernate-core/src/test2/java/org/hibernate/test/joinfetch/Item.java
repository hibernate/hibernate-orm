/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Item.java 6957 2005-05-31 04:21:58Z oneovthafew $
package org.hibernate.test.joinfetch;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Item {
	
	private String description;
	private Long id;
	private Category category;
	private Set bids = new HashSet();
	private Set comments = new HashSet();

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	Item() {}
	public Item(Category cat, String desc) { 
		description = desc; 
		category = cat;
	}

	public Set getBids() {
		return bids;
	}

	public void setBids(Set bids) {
		this.bids = bids;
	}

	public Set getComments() {
		return comments;
	}

	public void setComments(Set comments) {
		this.comments = comments;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

}
