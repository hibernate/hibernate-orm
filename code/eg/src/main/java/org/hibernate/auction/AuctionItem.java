//$Id: AuctionItem.java 7369 2005-07-04 03:18:34Z oneovthafew $
package org.hibernate.auction;

import java.util.Date;
import java.util.List;

/**
 * @author Gavin King
 */
public class AuctionItem extends Persistent {
	private String description;
	private String shortDescription;
	private List bids;
	private Bid successfulBid;
	private User seller;
	private Date ends;
	private int condition;
	public List getBids() {
		return bids;
	}

	public String getDescription() {
		return description;
	}

	public User getSeller() {
		return seller;
	}

	public Bid getSuccessfulBid() {
		return successfulBid;
	}

	public void setBids(List bids) {
		this.bids = bids;
	}

	public void setDescription(String string) {
		description = string;
	}

	public void setSeller(User user) {
		seller = user;
	}

	public void setSuccessfulBid(Bid bid) {
		successfulBid = bid;
	}

	public Date getEnds() {
		return ends;
	}

	public void setEnds(Date date) {
		ends = date;
	}
	
	public int getCondition() {
		return condition;
	}

	public void setCondition(int i) {
		condition = i;
	}

	public String toString() {
		return shortDescription + " (" + description + ": " + condition + "/10)";
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

}
