//$Id$
package org.hibernate.auction;

import java.util.Date;

/**
 * @author Gavin King
 */
public class AuctionInfo {
	private long id;
	private String description;
	private Date ends;
	private Float maxAmount;
	public String getDescription() {
		return description;
	}

	public Date getEnds() {
		return ends;
	}

	public long getId() {
		return id;
	}

	public Float getMaxAmount() {
		return maxAmount;
	}
	
	public AuctionInfo(long id, String description, Date ends, Float maxAmount) {
		this.id = id;
		this.description = description;
		this.ends = ends;
		this.maxAmount = maxAmount;
	}

}
