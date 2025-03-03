/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.composite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
//tag::identifiers-composite-generated-mapping-example[]
@Entity
class Event {

	@Id
	private EventId id;

	@Column(name = "event_key")
	private String key;

	@Column(name = "event_value")
	private String value;

	//Getters and setters are omitted for brevity
//end::identifiers-composite-generated-mapping-example[]

	public EventId getId() {
		return id;
	}

	public void setId(EventId id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
//tag::identifiers-composite-generated-mapping-example[]
}
//end::identifiers-composite-generated-mapping-example[]
