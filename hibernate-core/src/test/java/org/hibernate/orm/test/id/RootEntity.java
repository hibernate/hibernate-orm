/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@jakarta.persistence.Entity
public class RootEntity implements Serializable {

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	@Column(name = "universalid")// "uid" is a keyword in Oracle
	private long uid;

	public String description;

	@jakarta.persistence.OneToMany(mappedBy = "linkedRoot")
	private java.util.List<RelatedEntity> linkedEntities = new java.util.ArrayList<RelatedEntity>();

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public void setLinkedEntities(java.util.List<RelatedEntity> linkedEntities) {
		this.linkedEntities = linkedEntities;
	}
	public java.util.List<RelatedEntity> getLinkedEntities() {
		return linkedEntities;
	}

}
