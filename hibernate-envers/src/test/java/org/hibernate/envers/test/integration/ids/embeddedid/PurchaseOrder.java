package org.hibernate.envers.test.integration.ids.embeddedid;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import java.io.Serializable;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class PurchaseOrder implements Serializable {
	@Id
	@GeneratedValue
	private Integer id;

	@ManyToOne
	@JoinColumns({
						 @JoinColumn(name = "model", referencedColumnName = "model", nullable = true),
						 @JoinColumn(name = "version", referencedColumnName = "version", nullable = true),
						 @JoinColumn(name = "producer", referencedColumnName = "producer", nullable = true)
				 })
	private Item item;

	@Column(name = "NOTE")
	private String comment;

	public PurchaseOrder() {
	}

	public PurchaseOrder(Item item, String comment) {
		this.item = item;
		this.comment = comment;
	}

	public PurchaseOrder(Integer id, Item item, String comment) {
		this.id = id;
		this.item = item;
		this.comment = comment;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof PurchaseOrder) ) {
			return false;
		}

		PurchaseOrder that = (PurchaseOrder) o;

		if ( getComment() != null ? !getComment().equals( that.getComment() ) : that.getComment() != null ) {
			return false;
		}
		if ( getId() != null ? !getId().equals( that.getId() ) : that.getId() != null ) {
			return false;
		}
		if ( getItem() != null ? !getItem().equals( that.getItem() ) : that.getItem() != null ) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return "PurchaseOrder(id = " + id + ", item = " + item + ", comment = " + comment + ")";
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (item != null ? item.hashCode() : 0);
		result = 31 * result + (comment != null ? comment.hashCode() : 0);
		return result;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}