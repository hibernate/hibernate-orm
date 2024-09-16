/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.events;

import java.sql.Timestamp;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Vlad Mihalcea
 */
//tag::events-default-listener-mapping-example[]
@MappedSuperclass
public abstract class BaseEntity {

	private Timestamp createdOn;

	private Timestamp updatedOn;

	//Getters and setters are omitted for brevity

//end::events-default-listener-mapping-example[]

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public Timestamp getUpdatedOn() {
		return updatedOn;
	}

	void setUpdatedOn(Timestamp updatedOn) {
		this.updatedOn = updatedOn;
	}
//tag::events-default-listener-mapping-example[]
}
//end::events-default-listener-mapping-example[]
