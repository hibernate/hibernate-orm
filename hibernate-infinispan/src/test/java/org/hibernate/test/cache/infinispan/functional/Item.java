/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Item {
    private Long id;
    private String name;
    private String description;
    private Item owner;
    private Set<Item> items = new HashSet<Item>(  );
	private Item bagOwner;
	private List<Item> bagOfItems = new ArrayList<Item>(  );

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
}
