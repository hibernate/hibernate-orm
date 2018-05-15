/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.entity;
import javax.persistence.Entity;

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
