/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bidi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Gavin King
 */
public class Auction {
	private Long id;
	private String description;
	private List<Bid> bids = new ArrayList<Bid>();
	private Bid successfulBid;
	private Date end;

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public List<Bid> getBids() {
		return bids;
	}

	public void setBids(List<Bid> bids) {
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
