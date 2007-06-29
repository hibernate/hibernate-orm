//$Id: Bid.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.auction;

import java.util.Date;

/**
 * @author Gavin King
 */
public class Bid extends Persistent {
	private AuctionItem item;
	private float amount;
	private Date datetime;
	private User bidder;
	
	public AuctionItem getItem() {
		return item;
	}

	public void setItem(AuctionItem item) {
		this.item = item;
	}

	public float getAmount() {
		return amount;
	}

	public Date getDatetime() {
		return datetime;
	}

	public void setAmount(float f) {
		amount = f;
	}

	public void setDatetime(Date date) {
		datetime = date;
	}

	public User getBidder() {
		return bidder;
	}

	public void setBidder(User user) {
		bidder = user;
	}

	public String toString() {
		return bidder.getUserName() + " $" + amount;
	}
	
	public boolean isBuyNow() {
		return false;
	}

}
