//$Id: Bid.java 6793 2005-05-16 05:46:47Z oneovthafew $
package org.hibernate.test.joinfetch;
import java.util.Calendar;

/**
 * @author Gavin King
 */
public class Bid {
	
	private float amount;
	private Item item;
	private Calendar timestamp;
	private Long id;

	public float getAmount() {
		return amount;
	}
	public void setAmount(float amount) {
		this.amount = amount;
	}
	public Item getItem() {
		return item;
	}
	public void setItem(Item item) {
		this.item = item;
	}
	public Calendar getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}
	
	Bid() {}
	public Bid(Item item, float amount) {
		this.amount = amount;
		this.item = item;
		item.getBids().add(this);
		this.timestamp = Calendar.getInstance();
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

}
