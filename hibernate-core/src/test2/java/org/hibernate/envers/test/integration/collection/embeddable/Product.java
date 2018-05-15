/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.embeddable;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class Product {
	@Id
	private Integer id;

	private String name;

	@ElementCollection
	@CollectionTable(name = "items", joinColumns = @JoinColumn(name = "productId"))
	@OrderColumn(name = "position")
	private List<Item> items = new ArrayList<Item>();

	Product() {

	}

	Product(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		result = 31 * result + ( items != null ? items.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( object == null | getClass() != object.getClass() ) {
			return false;
		}

		Product that = (Product) object;
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( name != null ? !name.equals( that.name ) : that.name != null ) {
			return false;
		}
		return !( items != null ? !items.equals( that.items ) : that.items != null );
	}
}
