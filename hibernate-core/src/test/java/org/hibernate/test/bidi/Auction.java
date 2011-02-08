//$Id: Auction.java 5733 2005-02-14 15:56:06Z oneovthafew $
package org.hibernate.test.bidi;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Gavin King
 */
public class Auction {
	private Long id;
	private String description;
	private List bids = new ArrayList();
	private Bid successfulBid;
	private Date end;
	
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	public List getBids() {
		return bids;
	}
	public void setBids(List bids) {
		this.bids = bids;
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
	public Bid getSuccessfulBid() {
		return successfulBid;
	}
	public void setSuccessfulBid(Bid successfulBid) {
		this.successfulBid = successfulBid;
	}
}
