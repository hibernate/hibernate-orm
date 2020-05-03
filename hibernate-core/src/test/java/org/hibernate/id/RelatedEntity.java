/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.id;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "entity2")
public class RelatedEntity {

	@Id
	@GeneratedValue
	@Column(name = "universalid")// "uid" is a keywork in Oracle
	private long uid;

	@jakarta.persistence.ManyToOne
	private RootEntity linkedRoot;

	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}

	public void setLinkedRoot(RootEntity linkedRoot) {
		this.linkedRoot = linkedRoot;
	}
	public RootEntity getLinkedRoot() {
		return linkedRoot;
	}
}
