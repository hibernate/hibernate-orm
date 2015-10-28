/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gail Badner
 */
public class OtherItem {
	private Long id;
	// mapping added programmatically
	private long version;
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

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
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
