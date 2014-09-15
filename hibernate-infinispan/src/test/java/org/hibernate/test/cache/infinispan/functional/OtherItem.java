/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cache.infinispan.functional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gail Badner
 */
public class OtherItem {
	private Long id;
	private String name;
	private Item favoriteItem;
	private List<Item> bagOfItems = new ArrayList<Item>();

	public OtherItem() {
	}

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

	public List<Item> getBagOfItems() {
		return bagOfItems;
	}

	public void setBagOfItems(List<Item> bagOfItems) {
		this.bagOfItems = bagOfItems;
	}

	public void addItemToBag(Item item) {
		bagOfItems.add( item );
		item.getOtherItems().add( this );
	}
}
