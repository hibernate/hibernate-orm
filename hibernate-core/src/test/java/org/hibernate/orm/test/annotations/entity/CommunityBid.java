/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class CommunityBid extends Bid {
	private Starred communityNote;

	public Starred getCommunityNote() {
		return communityNote;
	}

	public void setCommunityNote(Starred communityNote) {
		this.communityNote = communityNote;
	}

}
