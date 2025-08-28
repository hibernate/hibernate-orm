/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
