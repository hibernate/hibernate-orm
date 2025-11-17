/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

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
