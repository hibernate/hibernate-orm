/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.criteria.query;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Jan Schatteman
 */
@Entity(name = "Phone")
public class Phone {
	@Id
	@GeneratedValue
	private Long id;

	private Boolean isSynced;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Boolean getSynced() {
		return isSynced;
	}

	public void setSynced(Boolean synced) {
		isSynced = synced;
	}
}
