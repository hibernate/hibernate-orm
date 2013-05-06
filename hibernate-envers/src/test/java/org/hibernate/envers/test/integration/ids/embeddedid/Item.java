package org.hibernate.envers.test.integration.ids.embeddedid;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class Item implements Serializable {
	@EmbeddedId
	private ItemId id;

	private Double price;

	public Item() {
	}

	public Item(ItemId id, Double price) {
		this.id = id;
		this.price = price;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Item) ) {
			return false;
		}

		Item item = (Item) o;

		if ( getId() != null ? !getId().equals( item.getId() ) : item.getId() != null ) {
			return false;
		}
		if ( getPrice() != null ? !getPrice().equals( item.getPrice() ) : item.getPrice() != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (price != null ? price.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Item(id = " + id + ", price = + " + price + ")";
	}

	public ItemId getId() {
		return id;
	}

	public void setId(ItemId id) {
		this.id = id;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}
}