/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bidi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Jan Schatteman
 */
public class SpecialAuction {
	private Long id;
	private String description;
	private List<AbstractBid> bids = new ArrayList<>();
	private AbstractBid successfulBid;
	private Date end;

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public List<AbstractBid> getBids() {
		return bids;
	}

	public void setBids(List<AbstractBid> bids) {
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

	public AbstractBid getSuccessfulBid() {
		return successfulBid;
	}

	public void setSuccessfulBid(AbstractBid successfulBid) {
		this.successfulBid = successfulBid;
	}
}
