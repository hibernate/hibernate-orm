/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.composite;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import jakarta.persistence.Embeddable;

/**
 * @author Vlad Mihalcea
 */
//tag::identifiers-composite-generated-mapping-example[]
@Embeddable
class EventId implements Serializable {

	private Integer category;

	private Timestamp createdOn;

	//Getters and setters are omitted for brevity
//end::identifiers-composite-generated-mapping-example[]

	public Integer getCategory() {
		return category;
	}

	public void setCategory(Integer category) {
		this.category = category;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

//tag::identifiers-composite-generated-mapping-example[]
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EventId that = (EventId) o;
		return Objects.equals(category, that.category) &&
				Objects.equals(createdOn, that.createdOn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(category, createdOn);
	}
}
//end::identifiers-composite-generated-mapping-example[]
