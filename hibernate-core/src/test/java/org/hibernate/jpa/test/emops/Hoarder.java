/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.emops;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * @author Gail Badner
 */
@Entity
public class Hoarder {
	@Id
	@GeneratedValue
	private Long id;
	private String name;

	@ManyToOne( cascade = { CascadeType.PERSIST, CascadeType.MERGE } )
	private Item favoriteItem;

	@OneToMany( cascade = { CascadeType.PERSIST, CascadeType.MERGE } )
	@JoinColumn
	private Set<Item> items = new HashSet<Item>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Item getFavoriteItem() {
		return favoriteItem;
	}

	public void setFavoriteItem(Item favoriteItem) {
		this.favoriteItem = favoriteItem;
	}

	public Set<Item> getItems() {
		return items;
	}

	public void setItems(Set<Item> items) {
		this.items = items;
	}

	@Override
	public String toString() {
		return "Hoarder{" +
				"id=" + id +
				", name='" + name + '\'' +
				", favoriteItem=" + favoriteItem +
				'}';
	}
}
