/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomanyassociationclass.nestedreference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Item {
	private Long id;
	// mapping for version is added programmatically
	private long version;
	private String name;
	private String description;
	private Item owner;
	private Set<Item> items = new HashSet<>();
	private Item bagOwner;
	private List<Item> bagOfItems = new ArrayList<>();
	private Set<OtherItem> otherItems = new HashSet<>();

	public Item() {}

	public Item( String name, String description ) {
		this.name = name;
		this.description = description;
	}

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

	public Item getOwner() {
		return owner;
	}

	public void setOwner( Item owner ) {
		this.owner = owner;
	}

	public Set<Item> getItems() {
		return items;
	}

	public void setItems( Set<Item> items ) {
		this.items = items;
	}

	public void addItem( Item item ) {
		item.setOwner( this );
		getItems().add( item );
	}

	public Item getBagOwner() {
		return bagOwner;
	}

	public void setBagOwner( Item bagOwner ) {
		this.bagOwner = bagOwner;
	}

	public List<Item> getBagOfItems() {
		return bagOfItems;
	}

	public void setBagOfItems( List<Item> bagOfItems ) {
		this.bagOfItems = bagOfItems;
	}

	public void addItemToBag( Item item ) {
		item.setBagOwner( this );
		getBagOfItems().add( item );
	}

	public Set<OtherItem> getOtherItems() {
		return otherItems;
	}

	public void setOtherItems(Set<OtherItem> otherItems) {
		this.otherItems = otherItems;
	}

	public void addOtherItem(OtherItem otherItem) {
		getOtherItems().add(otherItem);
		otherItem.getBagOfItems().add(this);
	}

	@Override
	public String toString() {
		return "Item{" +
				"id=" + id +
				", version=" + version +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", owner=" + owner +
				", items=" + items +
				", bagOwner=" + bagOwner +
				", bagOfItems=" + bagOfItems +
				", otherItems=" + otherItems +
				'}';
	}
}
